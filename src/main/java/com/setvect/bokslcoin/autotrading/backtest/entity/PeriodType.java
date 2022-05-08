package com.setvect.bokslcoin.autotrading.backtest.entity;

import lombok.Getter;

import java.time.LocalDateTime;

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

    /**
     * 예)
     * - PERIOD_60, 2022-03-11 10:22:34 => 2022-03-11 10:00:00 <br>
     * - PERIOD_240, 2022-03-11 05:11:00 => 2022-03-11 04:00:00 <br>
     *
     * @param timeUtc 기준 시간
     * @return 기준 시간을 캔들에 포함된 시간으로 변경함
     */
    public LocalDateTime fitDateTime(LocalDateTime timeUtc) {
        LocalDateTime lastCandleTime;
        switch (this) {
            case PERIOD_15:
                lastCandleTime = timeUtc.minusMinutes(timeUtc.getMinute() % 15).withSecond(0).withNano(0);
                break;
            case PERIOD_30:
                lastCandleTime = timeUtc.minusMinutes(timeUtc.getMinute() % 30).withSecond(0).withNano(0);
                break;
            case PERIOD_60:
                lastCandleTime = timeUtc.withMinute(0).withSecond(0).withNano(0);
                break;
            case PERIOD_240:
                lastCandleTime = timeUtc.minusHours(timeUtc.getHour() % 4).withMinute(0).withSecond(0).withNano(0);
                break;
            case PERIOD_1440:
                lastCandleTime = timeUtc.withHour(0).withMinute(0).withSecond(0).withNano(0);
                break;
            default:
                lastCandleTime = timeUtc;
        }
        return lastCandleTime;
    }
}
