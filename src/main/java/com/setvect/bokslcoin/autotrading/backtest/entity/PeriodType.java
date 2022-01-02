package com.setvect.bokslcoin.autotrading.backtest.entity;

import lombok.Getter;

/**
 * 분봉 주기
 */
@Getter
public enum PeriodType {
    PERIOD_1(1),
    PERIOD_15(15),
    PERIOD_30(30),
    PERIOD_60(60),
    PERIOD_240(240),
    PERIOD_1440(1440);

    private final int diffMinutes;

    PeriodType(int diffMinutes) {
        this.diffMinutes = diffMinutes;
    }
}
