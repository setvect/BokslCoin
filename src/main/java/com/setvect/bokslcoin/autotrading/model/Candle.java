package com.setvect.bokslcoin.autotrading.model;

import com.setvect.bokslcoin.autotrading.algorithm.websocket.TradeResult;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
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
    private Double openingPrice;
    /**
     * 고가
     */
    private Double highPrice;
    /**
     * 저가
     */
    private Double lowPrice;
    /**
     * 종가
     */
    private Double tradePrice;

    /**
     * 해당 캔들에서 마지막 틱이 저장된 시각
     */
    private Long timestamp;
    /**
     * 누적 거래 금액
     */
    private Double candleAccTradePrice;
    /**
     * 누적 거래량
     */
    private Double candleAccTradeVolume;

    public Candle(TradeResult tradeResult, PeriodType periodType) {
        market = tradeResult.getCode();
        candleDateTimeUtc = periodType.fitDateTime(tradeResult.getTradeDateTimeUtc());
        candleDateTimeKst = periodType.fitDateTime(tradeResult.getTradeDateTimeKst());
        openingPrice = tradeResult.getTradePrice();
        highPrice = tradeResult.getTotalPrice();
        lowPrice = 0.0;
        lowPrice = tradeResult.getTotalPrice();
        tradePrice = 0.0;
        tradePrice = tradeResult.getTotalPrice();
        timestamp = tradeResult.getTimestamp();
        candleAccTradeVolume = tradeResult.getTradeVolume();
        candleAccTradePrice = tradeResult.getTotalPrice();
    }

    public void change(TradeResult tradeResult) {
        timestamp = tradeResult.getTimestamp();
        changePrice(tradeResult.getTradePrice());
        candleAccTradeVolume += tradeResult.getTradeVolume();
        candleAccTradePrice += tradeResult.getTotalPrice();
    }

    /**
     * - 현재가격 변경
     * - 입력값이 고가보다 높으면 고가 변경
     * - 저가 보다 낮으면 저가 변경
     *
     * @param price 가격
     */
    private void changePrice(double price) {
        tradePrice = price;
        if (highPrice < price) {
            highPrice = price;
        }
        if (lowPrice > price) {
            lowPrice = price;
        }
    }
}
