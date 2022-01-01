package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis;

import lombok.Getter;
import lombok.Setter;

/**
 * 단위 수익 정보
 */
@Getter
@Setter
public class WinningRate {
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