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
public class CrawlerIncrementalTest {
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
    @SneakyThrows
    public void 증분클로링() {
        List<String> marketList = Arrays.asList("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-EOS", "KRW-ETC", "KRW-ADA", "KRW-MANA", "KRW-BAT", "KRW-BCH", "KRW-DOT");
        makeSaveDir();
        for (String market : marketList) {
            List<File> candleByMonth = crawlCandle(market);
            log.info("캔들 임시저장 파일: {}", candleByMonth);
            saveCandle(candleByMonth);
            createCandleGroup(market);
        }
    }


    /**
     * @param candleFiles 1분봉 데이터
     */
    private void saveCandle(List<File> candleFiles) throws IOException {
        // 파일명(날짜) 기준 오름차순 정렬
        List<File> sortedFiles = candleFiles.stream().sorted(Comparator.comparing(File::getName)).collect(Collectors.toList());

        for (File file : sortedFiles) {
            List<CandleMinute> candles = GsonUtil.GSON.fromJson(FileUtils.readFileToString(file, "utf-8"), new TypeToken<List<CandleMinute>>() {
            }.getType());
            Collections.reverse(candles);
            List<CandleEntity> entities = candles.stream().map(this::getCandleEntity).collect(Collectors.toList());
            candleRepository.saveAll(entities);
            log.info("저장 {}, {}건", file.getName(), entities.size());
        }
    }

    private CandleEntity getCandleEntity(Candle candle) {
        CandleEntity entity = new CandleEntity();
        entity.setMarket(candle.getMarket());
        entity.setCandleDateTimeUtc(candle.getCandleDateTimeUtc());
        entity.setCandleDateTimeKst(candle.getCandleDateTimeKst());
        entity.setPeriodType(PeriodType.P_1);
        entity.setOpeningPrice(candle.getOpeningPrice());
        entity.setLowPrice(candle.getLowPrice());
        entity.setHighPrice(candle.getHighPrice());
        entity.setTradePrice(candle.getTradePrice());
        return entity;
    }

    /**
     * 1분봉을 기준으로 15분, 30분, 60분, 240분(4시간), 1440분(하루) 캔들을 만듦<br>
     * UTC 기준으로 만듦. 예를 들어 하루 분봉은 우리나라 시간으로 09:00:00 ~ 다음날 08:59:59을 의미
     * 예)
     * - 1분봉 -> 15분봉
     * - 15분봉 -> 30분봉
     * - 30분봉 -> 60분봉
     * - 60분봉 -> 240분봉
     * - 240분봉 -> 1440분봉
     *
     * @param market 코인
     */
    private void createCandleGroup(String market) {
        List<PeriodType> targetPeriodList = Arrays.asList(PeriodType.P_15, PeriodType.P_30, PeriodType.P_60, PeriodType.P_240, PeriodType.P_1440);
        PeriodType basePeriod = PeriodType.P_1;

        for (PeriodType targetPeriod : targetPeriodList) {
            LocalDateTime startTime = fitDateTime(market, basePeriod, targetPeriod);
            DateRange groupRange = new DateRange(startTime, LocalDateTime.now(ZoneOffset.UTC));
            List<CandleEntity> last = candleRepository.findMarketPricePeriodBefore(market, targetPeriod, LocalDateTime.now(ZoneOffset.UTC), PageRequest.of(0, 1));
            // 마지막 병합 결과는 완전하지 않을 수 있으니 일딴 지우고 시작 한다.
            if (!last.isEmpty()) {
                candleRepository.delete(last.get(0));
            }

            int count = 0;
            while (true) {
                LocalDateTime end = startTime.plusMinutes(targetPeriod.getDiffMinutes()).minusSeconds(1);
                if (!groupRange.isBetween(end)) {
                    break;
                }

                List<CandleEntity> smallCandle = candleRepository.findMarketPrice(market, basePeriod, startTime, end);
                startTime = startTime.plusMinutes(targetPeriod.getDiffMinutes());
                if (smallCandle.isEmpty()) {
                    continue;
                }
                CandleEntity upperCandle = mergeCandle(smallCandle);
                upperCandle.setMarket(market);
                upperCandle.setPeriodType(targetPeriod);

                candleRepository.save(upperCandle);
                if (count % 100 == 0) {
                    log.info("Count: {}, Period: {}, CandleDateTimeUtc: {}", count, upperCandle.getPeriodType(), upperCandle.getCandleDateTimeUtc());
                }
                count++;
            }
            log.info("저장. 코인:{}, 기간:{}-{} 병합 방식: {} -> {}, {}건",
                    market, groupRange.getFromDateTimeFormat(), groupRange.getToDateTimeFormat(), basePeriod, targetPeriod, count);
            basePeriod = targetPeriod;
        }
    }

    /**
     * basePeriod 분봉을 이용해  -> targetPeriod 분봉을 만듦기 위해 필요한 시작 시간을 제공
     *
     * @param market       코인
     * @param basePeriod   병합을 하기 위해 미리 계산된 분봉 주기
     * @param targetPeriod 병합 할 분봉 주기
     * @return basePeriod 시작 시간을 제공
     */
    private LocalDateTime fitDateTime(String market, PeriodType basePeriod, PeriodType targetPeriod) {
        List<CandleEntity> last = candleRepository.findMarketPricePeriodBefore(market, targetPeriod, LocalDateTime.now(ZoneOffset.UTC), PageRequest.of(0, 1));

        if (!last.isEmpty()) {
            return last.get(0).getCandleDateTimeUtc();
        }

        LocalDateTime lastCandleTime;
        List<CandleEntity> afterList = candleRepository.findMarketPricePeriodAfter(market, basePeriod, MINIMUM_CANDLE_DATE, PageRequest.of(0, 1));
        if (afterList.isEmpty()) {
            throw new RuntimeException(String.format("시세 정보가 없습니다. market: %s, Base Period: %s, Target Period: %s", market, basePeriod, targetPeriod));
        }

        LocalDateTime timeUtc = afterList.get(0).getCandleDateTimeUtc();
        return targetPeriod.fitDateTime(timeUtc);
    }


    /**
     * 봉 데이터를 목록을 병합하여 병합된 봉 데이터를 만듦
     *
     * @param candleList 봉 데이터
     * @return 병합된 봉 데이터
     */
    private CandleEntity mergeCandle(List<CandleEntity> candleList) {
        CandleEntity period = new CandleEntity();
        CandleEntity first = candleList.get(0);
        CandleEntity last = candleList.get(candleList.size() - 1);
        period.setOpeningPrice(first.getOpeningPrice());
        period.setCandleDateTimeUtc(first.getCandleDateTimeUtc());
        period.setCandleDateTimeKst(first.getCandleDateTimeKst());
        period.setTradePrice(last.getTradePrice());
        double low = candleList.stream().mapToDouble(CandleEntity::getLowPrice).min().orElse(0);
        period.setLowPrice(low);
        double high = candleList.stream().mapToDouble(CandleEntity::getHighPrice).max().orElse(0);
        period.setHighPrice(high);
        return period;
    }

    /**
     * @param market 코인명
     * @return 1분봉 데이터 월단위 저장 파일
     */
    @SneakyThrows
    private List<File> crawlCandle(String market) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<CandleEntity> lastCandle = candleRepository.findMarketPricePeriodBefore(market, PeriodType.P_1, now, PageRequest.of(0, 1));
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
            log.info("{} - {} 번째. last: {},  current: {}", market, i, lastSaveCandle, to);
            // API 횟수 제한 때문에 딜레이 적용
            TimeUnit.MILLISECONDS.sleep(500);
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

    private void makeSaveDir() {
        if (SAVE_DIR_MINUTE.exists()) {
            return;
        }
        SAVE_DIR_MINUTE.mkdirs();
    }
}

