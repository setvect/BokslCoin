package com.setvect.bokslcoin.autotrading.backtest.mais.multi;

import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@ToString
public class TestAnalysisMulti {
    private String comment;
    /**
     * 코인별 시세 정보
     * Key: Market(KRW-BTC, KRW-ETH, ...)
     */
    private Map<String, YieldMdd> coin = new HashMap<>();


    /**
     * 코인별 투자 수익률 정보
     * Key: Market(KRW-BTC, KRW-ETH, ...)
     */
    private Map<String, CoinInvestment> coinInvestment = new HashMap<>();

    /**
     * 전체 수익률
     */
    private TotalYield total;


    public void addCoinYieldMdd(String market, YieldMdd yieldMdd) {
        coin.put(market, yieldMdd);
    }

    public YieldMdd getCoinYield(String market) {
        return coin.get(market);
    }

    public void addCoinInvestment(String market, CoinInvestment yield) {
        coinInvestment.put(market, yield);
    }

    public CoinInvestment getCoinInvestment(String market) {
        return coinInvestment.get(market);
    }

    /**
     * 수익률과 MDD
     */
    @Getter
    @Setter
    public static class YieldMdd {
        /**
         * 수익률
         */
        private double yield;
        /**
         * 최대 낙폭
         */
        private double mdd;
    }

    /**
     * 코인별 수익률
     */
    @Getter
    @Setter
    public static class TotalYield extends YieldMdd {
        /**
         * 수익 카운트
         */
        private int gainCount;
        /**
         * 이익 카운트
         */
        private int lossCount;

        /**
         * 분석 일자
         */
        private int dayCount;

        /**
         * @return 총 매매 횟수 (매수-매도가 한쌍)
         */
        public int getTradeCount() {
            return gainCount + lossCount;
        }

        /**
         * @return 총 매매에서 이익을 본 비율
         */
        public double getWinRate() {
            return (double) gainCount / getTradeCount();
        }

        /**
         * @return 연복리
         */
        public double getCagr() {
            return ApplicationUtil.getCagr(1.0, 1 + getYield(), dayCount);
        }
    }


    /**
     * 코인별 수익률
     */
    @Getter
    @Setter
    public static class CoinInvestment {
        /**
         * 수익 카운트
         */
        private int gainCount;
        /**
         * 이익 카운트
         */
        private int lossCount;
        /**
         * 수익 합계
         */
        private double invest;

        /**
         * @return 총 매매 횟수 (매수-매도가 한쌍)
         */
        public int getTradeCount() {
            return gainCount + lossCount;
        }

        /**
         * @return 총 매매에서 이익을 본 비율
         */
        public double getWinRate() {
            return (double) gainCount / getTradeCount();
        }
    }
}
