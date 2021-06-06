package com.setvect.bokslcoin.autotrading.model;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class CandleDay extends Candle {
    /**
     * 전일 종가(UTC 0시 기준)
     */
    private Double prevClosingPrice;
    /**
     * 전일 종가 대비 변화 금액
     */
    private Double changePrice;
    /**
     * 전일 종가 대비 변화량
     */
    private Double changeRate;
    /**
     * 종가 환산 화폐 단위로 환산된 가격(요청에 convertingPriceUnit 파라미터 없을 시 해당 필드 포함되지 않음.)
     */
    private Double convertedTradePrice;
}
