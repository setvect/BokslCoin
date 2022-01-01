package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis;

import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 코인별 수익률
 */
@Getter
@Setter
@ToString
public class TotalYield extends YieldMdd {
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