package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis;

import com.setvect.bokslcoin.autotrading.backtest.entity.MabsConditionEntity;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 멀티 코인 매매 백테스트 분석 결과
 */
@Getter
@Builder
public class AnalysisReportResult {
    /**
     * 리포트 조건
     */
    private AnalysisMultiCondition condition;
    /**
     * 종목별 매매 조건
     */
    private List<MabsConditionEntity> conditionList;
    /**
     * 매매 이력
     */
    private List<MabsTradeReportItem> tradeHistory;
    /**
     * 전체 수익 결과
     */
    private TotalYield totalYield;

    /**
     * 코인별 승률
     */
    private Map<String, WinningRate> coinWinningRate;

    /**
     * 코인별 Buy&Hold 수익률
     */
    private MultiCoinHoldYield multiCoinHoldYield;

    /**
     * @return 코인 이름
     */
    public Set<String> getMarkets() {
        return conditionList.stream().map(MabsConditionEntity::getMarket).collect(Collectors.toSet());
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

    @Getter
    @Setter
    @ToString
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
            int tradeCount = getTradeCount();
            if (tradeCount == 0) {
                return 0.0;
            }
            return (double) gainCount / tradeCount;
        }

        /**
         * @return 연복리
         */
        public double getCagr() {
            return ApplicationUtil.getCagr(1.0, 1 + getYield(), dayCount);
        }

        /**
         * 수익 카운트 증가
         */
        public void incrementGainCount() {
            gainCount++;
        }

        /**
         * 손실 카운트 증가
         */
        public void incrementLossCount() {
            lossCount++;
        }
    }
    /**
     * 동일 비중 보유  수익률
     */
    @Getter
    @Builder
    public static class MultiCoinHoldYield {
        /**
         * Key: market, Value: 수익률
         */
        private Map<String, YieldMdd> coinByYield;
        /**
         * 합계 수익률
         */
        private YieldMdd sumYield;
    }
    /**
     * 단위 수익 정보
     */
    @Getter
    @Setter
    public static class WinningRate {
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

