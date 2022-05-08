package com.setvect.bokslcoin.autotrading.algorithm;

import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import com.setvect.bokslcoin.autotrading.util.MathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommonTradeHelper {
    /**
     * @param moveListCandle 캔들 시세
     * @param range          계산할 범위
     * @return 종가 기준 이동평균 값
     */
    public static double getMa(List<Candle> moveListCandle, int range) {
        if (moveListCandle.size() < range) {
            return 0;
        }
        List<Double> values = moveListCandle.stream().map(Candle::getTradePrice).collect(Collectors.toList());
        return MathUtil.getAverage(values, 0, range);
    }

    /**
     * @param moveListCandle 캔들 시세(최근 시세가 앞)
     * @param range          계산할 범위
     * @return 종가 기준 가중 이동평균 값
     */
    public static double getMaWeight(List<Candle> moveListCandle, int range) {
        if (moveListCandle.size() < range) {
            return 0;
        }
        List<Double> values = moveListCandle.stream().map(Candle::getTradePrice).collect(Collectors.toList());
        return MathUtil.getAverageWeight(values, 0, range);
    }

    /**
     * @param candleService ...
     * @param market        코인(KRW-BTC, KRW-ETH, ...)
     * @param tradePeriod   매매 주기
     * @param periodCount   주기
     * @return 캔들(최근 순서대로)
     */
    public static List<Candle> getCandles(CandleService candleService, String market, PeriodType tradePeriod, int periodCount) {
        List<Candle> moveListCandle = new ArrayList<>();
        switch (tradePeriod) {
            case P_15:
            case P_30:
            case P_60:
            case P_240:
                List<CandleMinute> candles = candleService.getMinute(tradePeriod.getDiffMinutes(), market, periodCount);
                moveListCandle.addAll(candles);
                break;
            case P_1440:
                List<CandleDay> candlesDay = candleService.getDay(market, periodCount);
                moveListCandle.addAll(candlesDay);
                break;
        }
        return moveListCandle;
    }
}
