package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis;

import lombok.Getter;
import lombok.Setter;

/**
 * 수익률과 MDD
 */
@Getter
@Setter
public class YieldMdd {
    /**
     * 수익률
     */
    private double yield;
    /**
     * 최대 낙폭
     */
    private double mdd;
}
