package com.setvect.bokslcoin.autotrading.algorithm.websocket;

import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 웹서비스 체결 모델
 * https://docs.upbit.com/docs/upbit-quotation-websocket
 */
@Getter
@Builder
@ToString
public class TradeResult {
    private String type;
    private String code;
    private double tradePrice;
    private double tradeVolume;
    private long timestamp;
    private LocalDate tradeDate;
    private LocalTime tradeTime;
    private double prevClosingPrice;

    public LocalDateTime getTradeDateTimeUtc() {
        return LocalDateTime.of(tradeDate, tradeTime);
    }

    public LocalDateTime getTradeDateTimeKst() {
        return getTradeDateTimeUtc().plusHours(9);
    }

    /**
     * @return 거래량 * 금액
     */
    public double getTotalPrice() {
        return tradePrice * tradeVolume;
    }

    /**
     * @return 일일 수익률
     */
    public double getYieldDay() {
        return ApplicationUtil.getYield(prevClosingPrice, tradePrice);
    }

    /**
     * @return 현 서버 타임스템프 - 시세 타임스템프
     */
    public long getTimestampDiff() {
        return System.currentTimeMillis() - timestamp;
    }
}

