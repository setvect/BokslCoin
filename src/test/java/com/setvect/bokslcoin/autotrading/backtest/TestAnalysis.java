package com.setvect.bokslcoin.autotrading.backtest;

import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class TestAnalysis {
    private String comment;
    private double coinYield;
    // 코인 차트 기준 최대 낙폭
    private double coinMdd;
    private double realYield;
    // 실 투자 기준 최대 낙폭
    private double realMdd;

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
        return ApplicationUtil.getCagr(1.0, 1 + realYield, dayCount);
    }
}
