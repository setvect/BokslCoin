package com.setvect.bokslcoin.autotrading.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class Candle {
    /**
     * 마켓명
     */
    private String market;
    /**
     * 캔들 기준 시각(UTC 기준)
     */
    private LocalDateTime candleDateTimeUtc;
    /**
     * 캔들 기준 시각(KST 기준)
     */
    private LocalDateTime candleDateTimeKst;
    /**
     * 시가
     */
    private Double openingPrice = Double.valueOf(0);
    /**
     * 고가
     */
    private Double highPrice = Double.valueOf(0);
    /**
     * 저가
     */
    private Double lowPrice = Double.valueOf(0);
    /**
     * 종가
     */
    private Double tradePrice = Double.valueOf(0);

    /**
     * 해당 캔들에서 마지막 틱이 저장된 시각
     */
    private Long timestamp;
    /**
     * 누적 거래 금액
     */
    private Double candleAccTradePrice = Double.valueOf(0);
    /**
     * 누적 거래량
     */
    private Double candleAccTradeVolume = Double.valueOf(0);
}
