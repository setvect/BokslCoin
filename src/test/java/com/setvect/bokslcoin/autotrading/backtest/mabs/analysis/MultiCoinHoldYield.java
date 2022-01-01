package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 동일 비중 보유  수익률
 */
@Getter
@Builder
public class MultiCoinHoldYield {
    /**
     * Key: market, Value: 수익률
     */
    private Map<String, YieldMdd> coinByYield;
    /**
     * 합계 수익률
     */
    private YieldMdd sumYield;
}
