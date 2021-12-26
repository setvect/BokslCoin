package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis;

import com.setvect.bokslcoin.autotrading.algorithm.AskReason;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import lombok.Getter;
import lombok.Setter;

/**
 * 주기별 매매 정보
 */
@Setter
@Getter
public class MabsMultiBacktestRow {
    private Candle candle;
    private TradeType tradeEvent;
    /**
     * 매수 체결 가격
     */
    private double bidPrice;
    /**
     * 매도 체결 가격
     */
    private double askPrice;
    /**
     * 매수금액
     */
    private double buyAmount;
    /**
     * 전체 코인 매수 금액
     */
    private double buyTotalAmount;
    /**
     * 현금
     */
    private double cash;
    /**
     * 매도 이유
     */
    private AskReason askReason;

    /**
     * 최고 수익률
     */
    private double highYield;
    /**
     * 최저 수익률
     */
    private double lowYield;
    /**
     * 단기 이동평균
     */
    private double maShort;
    /**
     * 장기 이동평균
     */
    private double maLong;

    public MabsMultiBacktestRow(Candle candle) {
        this.candle = candle;
    }

    // 투자 수익
    public double getGains() {
        if (tradeEvent == TradeType.SELL) {
            return buyAmount * getRealYield();
        }
        return 0;
    }

    /**
     * @return 현금 + 전체코인 매수금액
     */
    public double getFinalResult() {
        return getBuyTotalAmount() + cash;
    }

    /**
     * @return 실현 수익률
     */
    public double getRealYield() {
        if (tradeEvent == TradeType.SELL) {
            return (askPrice / bidPrice) - 1;
        }
        return 0;
    }
}
