package com.setvect.bokslcoin.autotrading.model;

import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class CandleDay extends Candle {
    /**
     * 전일 종가(UTC 0시 기준)
     */
    private double prevClosingPrice;
    /**
     * 전일 종가 대비 변화 금액
     */
    private double changePrice;
    /**
     * 전일 종가 대비 변화량
     */
    private double changeRate;
    /**
     * 종가 환산 화폐 단위로 환산된 가격(요청에 convertingPriceUnit 파라미터 없을 시 해당 필드 포함되지 않음.)
     */
    private double convertedTradePrice;

    /**
     * @return 전일 종가대비 수익률
     */
    public double getYield() {
        if (prevClosingPrice == 0) {
            return 0;
        }
        return ApplicationUtil.getYield(prevClosingPrice, getTradePrice());
    }
}
