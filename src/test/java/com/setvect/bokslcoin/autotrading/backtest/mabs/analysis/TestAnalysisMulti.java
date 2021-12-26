package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis;

import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class TestAnalysisMulti {
    private String comment;
    /**
     * 코인별 시세 정보
     * Key: Market(KRW-BTC, KRW-ETH, ...)
     */
    private YieldMdd coinByYield;

    /**
     * 균등 배분 시 코인 수익률, MDD
     */
    private YieldMdd coinTotalYield;

    /**
     * 코인별 투자 수익률 정보
     */
    private CoinInvestment investmentInvestment;
    /**
     * 전체 투자 수익률, MDD
     */
    private TotalYield investmentTotalYield;

    public void addCoinYieldMdd(YieldMdd yieldMdd) {
        coinByYield = yieldMdd;
    }

    public YieldMdd getCoinTotalYield() {
        return coinByYield;
    }


    public void addCoinInvestment(CoinInvestment yield) {
        investmentInvestment = yield;
    }

    public CoinInvestment getInvestmentInvestment() {
        return investmentInvestment;
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
