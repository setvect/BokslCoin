package com.setvect.bokslcoin.autotrading.algorithm;

import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

public class CommonTradeHelper {
    /**
     * @param moveListCandle 캔들 시세
     * @param periodCount    가져올 갯수
     * @return 종가 기준 이동평균 값
     */
    public static double getMa(List<Candle> moveListCandle, int periodCount) {
        if (moveListCandle.size() < periodCount) {
            return 0;
        }
        OptionalDouble val = moveListCandle.stream()
                .limit(periodCount).mapToDouble(c -> c.getTradePrice()).average();
        return val.isPresent() ? val.getAsDouble() : 0;
    }

    /**
     * @param moveListCandle 캔들 시세(최근 시세가 앞)
     * @param periodCount    가져올 갯수
     * @return 종가 기준 가중 이동평균 값
     */
    public static double getMaWeight(List<Candle> moveListCandle, int periodCount) {
        if (moveListCandle.size() < periodCount) {
            return 0;
        }

        int totalCount = 0;
        double totalSum = 0;
        for (int i = 0; i < periodCount; i++) {
            Double tradePrice = moveListCandle.get(i).getTradePrice();
            int weight = periodCount - i;
            totalCount += weight;
            totalSum += tradePrice * weight;
        }
        double v = totalSum / totalCount;
        return v;
    }


    public static List<Candle> getCandles(CandleService candleService, String market, TradePeriod tradePeriod, int longPeriod) {
        List<Candle> moveListCandle = new ArrayList<>();
        switch (tradePeriod) {
            case P_60:
                List<CandleMinute> t1 = candleService.getMinute(60, market, longPeriod);
                moveListCandle.addAll(t1);
            case P_240:
                List<CandleMinute> t2 = candleService.getMinute(240, market, longPeriod);
                moveListCandle.addAll(t2);
            case P_1440:
                List<CandleDay> t3 = candleService.getDay(market, longPeriod);
                moveListCandle.addAll(t3);
        }
        return moveListCandle;
    }
}
