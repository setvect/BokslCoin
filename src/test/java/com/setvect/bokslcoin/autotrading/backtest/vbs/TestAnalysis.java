package com.setvect.bokslcoin.autotrading.backtest.vbs;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public  class TestAnalysis {
    private String comment;
    private double coinYield;
    // 코인 차트 기준 최대 낙폭
    private double coinMdd;
    private double realYield;
    // 실 투자 기준 최대 낙폭
    private double realMdd;

}
