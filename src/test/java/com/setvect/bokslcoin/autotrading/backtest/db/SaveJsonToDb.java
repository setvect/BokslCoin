package com.setvect.bokslcoin.autotrading.backtest.db;

import com.google.common.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.backtest.entity.CandleEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepository;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
@Rollback(false)
public class SaveJsonToDb {
    @Autowired
    private CandleRepository candleRepository;

    @Test
    public void test() throws IOException {
        candleRepository.deleteAll();
        List<String> markets = Arrays.asList("KRW-BTC", "KRW-XRP", "KRW-ETH", "KRW-EOS", "KRW-ETC");
        for (String market : markets) {
            restore(market);
        }
    }

    private void restore(String market) throws IOException {
        File dir = new File("./craw-data/minute");
        File[] files = dir.listFiles(n -> n.getName().contains(market));
        Arrays.sort(files, (a, b)->a.getName().compareTo(b.getName()));

        List<CandleMinute> p15 = new ArrayList<>();
        List<CandleMinute> p30 = new ArrayList<>();
        List<CandleMinute> p60 = new ArrayList<>();
        List<CandleMinute> p240 = new ArrayList<>();
        List<CandleMinute> p1440 = new ArrayList<>();

        int minute15 = -1;
        int minute30 = -1;
        int hour = -1;
        int hour4 = -1;
        int day = -1;

        for (File dataFile : files) {
            List<CandleMinute> candles = GsonUtil.GSON.fromJson(FileUtils.readFileToString(dataFile, "utf-8"), new TypeToken<List<CandleMinute>>() {
            }.getType());
            Collections.reverse(candles);

            for (CandleMinute candle : candles) {
                LocalDateTime candleDateTimeUtc = candle.getCandleDateTimeUtc();

                // 15분 합계
                if (candleDateTimeUtc.getMinute() / 15 != minute15) {
                    if (!p15.isEmpty()) {
                        Candle mergeCandle = getCandle(p15);
                        CandleEntity entity = getCandleEntity(market, mergeCandle, PeriodType.PERIOD_15);
                        candleRepository.save(entity);
                        p15.clear();
                    }
                    minute15 = candleDateTimeUtc.getMinute() / 15;
                }

                // 30분 합계
                if (candleDateTimeUtc.getMinute() / 30 != minute30) {
                    if (!p30.isEmpty()) {
                        Candle mergeCandle = getCandle(p30);
                        CandleEntity entity = getCandleEntity(market, mergeCandle, PeriodType.PERIOD_30);
                        candleRepository.save(entity);
                        p30.clear();
                    }
                    minute30 = candleDateTimeUtc.getMinute() / 30;
                }

                // 1시간 합계
                if (candleDateTimeUtc.getHour() != hour) {
                    if (!p60.isEmpty()) {
                        Candle mergeCandle = getCandle(p60);
                        CandleEntity entity = getCandleEntity(market, mergeCandle, PeriodType.PERIOD_60);
                        candleRepository.save(entity);
                        p60.clear();
                    }
                    hour = candleDateTimeUtc.getHour();
                }

                // 4시간 합계
                if (candleDateTimeUtc.getHour() / 4 != hour4) {
                    if (!p240.isEmpty()) {
                        Candle mergeCandle = getCandle(p240);
                        CandleEntity entity = getCandleEntity(market, mergeCandle, PeriodType.PERIOD_240);
                        candleRepository.save(entity);
                        p240.clear();
                    }
                    hour4 = candleDateTimeUtc.getHour() / 4;
                }

                // 하루
                if (candleDateTimeUtc.getDayOfMonth() != day) {
                    if (!p1440.isEmpty()) {
                        Candle mergeCandle = getCandle(p1440);
                        CandleEntity entity = getCandleEntity(market, mergeCandle, PeriodType.PERIOD_1440);
                        candleRepository.save(entity);
                        p1440.clear();
                    }
                    day = candleDateTimeUtc.getDayOfMonth();
                }

                p1440.add(candle);
                p240.add(candle);
                p60.add(candle);
                p30.add(candle);
                p15.add(candle);

                CandleEntity entity = getCandleEntity(market, candle, PeriodType.PERIOD_1);
                candleRepository.save(entity);
            }

            log.info(String.format("load data file: %s", dataFile.getName()));
        }
    }

    private Candle getCandle(List<CandleMinute> accList) {
        Candle period = new Candle();
        Candle first = accList.get(0);
        Candle last = accList.get(accList.size() - 1);
        period.setOpeningPrice(first.getOpeningPrice());
        period.setCandleDateTimeUtc(first.getCandleDateTimeUtc());
        period.setCandleDateTimeKst(first.getCandleDateTimeKst());
        period.setTradePrice(last.getTradePrice());
        double low = accList.stream().mapToDouble(p -> p.getLowPrice()).min().getAsDouble();
        period.setLowPrice(low);
        double high = accList.stream().mapToDouble(p -> p.getHighPrice()).max().getAsDouble();
        period.setHighPrice(high);
        return period;
    }

    private CandleEntity getCandleEntity(String market, Candle candle, PeriodType period) {
        CandleEntity entity = new CandleEntity();
        entity.setMarket(market);
        entity.setCandleDateTimeUtc(candle.getCandleDateTimeUtc());
        entity.setCandleDateTimeKst(candle.getCandleDateTimeKst());
        entity.setPeriodType(period);
        entity.setOpeningPrice(candle.getOpeningPrice());
        entity.setLowPrice(candle.getLowPrice());
        entity.setHighPrice(candle.getHighPrice());
        entity.setTradePrice(candle.getTradePrice());
        return entity;
    }
}
