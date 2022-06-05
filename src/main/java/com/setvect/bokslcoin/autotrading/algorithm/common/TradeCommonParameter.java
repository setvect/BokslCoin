package com.setvect.bokslcoin.autotrading.algorithm.common;

import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TradeCommonParameter {
    private int candleLoadCount;
    private List<String> markets;
    private PeriodType periodType;
    private int maxBuyCount;
    private double investRatio;
}
