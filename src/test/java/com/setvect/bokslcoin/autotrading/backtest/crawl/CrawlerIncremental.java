package com.setvect.bokslcoin.autotrading.backtest.crawl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.setvect.bokslcoin.autotrading.TestCommonUtil;
import com.setvect.bokslcoin.autotrading.backtest.entity.CandleEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepository;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 증분 데이터 수집 후 DB 저장
 */
@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class CrawlerIncremental {
    /**
     * 캔들 수집 최소 날짜. 해당 날짜 이전에는 캔들 데이터가 없다고 가정
     */
    public static final LocalDateTime MINIMUM_CANDLE_DATE = LocalDateTime.of(2015, 1, 1, 0, 0, 0).atZone(ZoneOffset.UTC).toLocalDateTime();
    private static final File SAVE_DIR_MINUTE = new File("./craw-data/temp");

    @Autowired
    private CandleService candleService;

    @Autowired
    private CandleRepository candleRepository;

    @Test
    public void 증분클로링() throws IOException, InterruptedException {
//        List<String> marketList = Arrays.asList("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-EOS", "KRW-ETC", "KRW-ADA");
        List<String> marketList = Arrays.asList("KRW-BTC");
        makeSaveDir(SAVE_DIR_MINUTE);

        for (String market : marketList) {
            List<File> candleByMonth = crawlCandle(market);
            // TODO DB 등록
            log.info("캔들 임시저장 파일: {}", candleByMonth);
            DateRange range = saveCandle(candleByMonth);
        }
    }

    /**
     * @param candleFiles 1분봉 데이터
     * @return 저장 데이터 시작 및 종료 범위(UTC)
     */
    private DateRange saveCandle(List<File> candleFiles) throws IOException {
        // 파일명(날짜) 기준 오름차순 정렬
        List<File> sortedFiles = candleFiles.stream().sorted(Comparator.comparing(File::getName)).collect(Collectors.toList());

        LocalDateTime start = null;
        LocalDateTime end = LocalDateTime.now();

        for (File file : sortedFiles) {
            List<CandleMinute> candles = GsonUtil.GSON.fromJson(FileUtils.readFileToString(file, "utf-8"), new TypeToken<List<CandleMinute>>() {
            }.getType());
            Collections.reverse(candles);

            if (start == null && !candles.isEmpty()) {
                start = candles.get(0).getCandleDateTimeUtc();
            }

            List<CandleEntity> entities = candles.stream().map(p -> getCandleEntity(p, PeriodType.PERIOD_1)).collect(Collectors.toList());
            candleRepository.saveAll(entities);
            log.info("저장 {} - {}건", file.getName(), entities.size());
        }
        return new DateRange(start, end);
    }

    private CandleEntity getCandleEntity(Candle candle, PeriodType period) {
        CandleEntity entity = new CandleEntity();
        entity.setMarket(candle.getMarket());
        entity.setCandleDateTimeUtc(candle.getCandleDateTimeUtc());
        entity.setCandleDateTimeKst(candle.getCandleDateTimeKst());
        entity.setPeriodType(period);
        entity.setOpeningPrice(candle.getOpeningPrice());
        entity.setLowPrice(candle.getLowPrice());
        entity.setHighPrice(candle.getHighPrice());
        entity.setTradePrice(candle.getTradePrice());
        return entity;
    }

    /**
     * @param market 코인
     * @return 1분봉 데이터 월단위 저장 파일
     */
    @SneakyThrows
    private List<File> crawlCandle(String market) {
        List<CandleEntity> lastCandle = candleRepository.findMarketPricePeriod(market, PeriodType.PERIOD_1, LocalDateTime.now(ZoneOffset.UTC), PageRequest.of(0, 1));
        LocalDateTime lastSaveCandle = lastCandle.isEmpty() ? MINIMUM_CANDLE_DATE : lastCandle.get(0).getCandleDateTimeUtc();

        LocalDateTime to = null;
        int month = LocalDateTime.now(ZoneId.of("UTC")).getMonth().getValue();
        List<CandleMinute> acc = new ArrayList<>();
        List<File> candleFiles = new ArrayList<>();
        for (int i = 0; ; i++) {
            List<CandleMinute> data;
            try {
                data = candleService.getMinute(1, market, 200, to);
            } catch (Exception e) {
                i--;
                log.info("{}, 다시 실행", e.getMessage());
                TimeUnit.MILLISECONDS.sleep(500);
                continue;
            }
            if (data.isEmpty()) {
                break;
            }
            boolean end = false;
            for (CandleMinute d : data) {
                LocalDateTime candleDateTimeUtc = d.getCandleDateTimeUtc();
                if (lastSaveCandle.isAfter(candleDateTimeUtc) || lastSaveCandle.isEqual(candleDateTimeUtc)) {
                    end = true;
                    break;
                }

                if (candleDateTimeUtc.getMonth().getValue() != month) {
                    File f = saveFileMinute(market, acc);
                    candleFiles.add(f);
                    acc.clear();
                    month = candleDateTimeUtc.getMonth().getValue();
                }
                acc.add(d);
            }
            if (end) {
                break;
            }
            to = data.get(data.size() - 1).getCandleDateTimeUtc();
            log.info("{} 번째. last: {},  current: {}", i, lastCandle, to);
            // API 횟수 제한 때문에 딜레이 적용
            TimeUnit.MILLISECONDS.sleep(150);
        }
        if (!acc.isEmpty()) {
            File f = saveFileMinute(market, acc);
            candleFiles.add(f);
        }
        return candleFiles;
    }

    private File saveFileMinute(String market, List<CandleMinute> acc) throws IOException {
        Gson gson = TestCommonUtil.getGson();
        String fileName = String.format("%s-minute(%s)", market, DateUtil.format(acc.get(0).getCandleDateTimeUtc(), "yyyy-MM"));
        File saveFile = File.createTempFile(fileName, ".json", SAVE_DIR_MINUTE);
        try (FileWriter writer = new FileWriter(saveFile)) {
            gson.toJson(acc, writer);
        }
        System.out.printf("저장: %s%n", saveFile.getAbsolutePath());
        return saveFile;
    }

    private void makeSaveDir(File dir) {
        if (dir.exists()) {
            return;
        }
        dir.mkdirs();
    }
}

