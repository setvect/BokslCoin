package com.setvect.bokslcoin.autotrading.backtest.mabs.multi;

import com.setvect.bokslcoin.autotrading.algorithm.AskReason;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * 주기별 매매 정보
 */
@Setter
@Getter
public class MabsMultiBacktestRow {
    public enum TradeEvent {
        BUY, SELL
    }

    private Candle candle;
    private TradeEvent tradeEvent;
    // 매수 체결 가격
    private double bidPrice;
    // 매도 체결 가격
    private double askPrice;
    // 매수금액
    private double buyAmount;
    // 전체 코인 매수 금액
    private double buyTotalAmount;
    // 현금
    private double cash;
    // 매매 수수료
    private double feePrice;
    private AskReason askReason;

    /**
     * 최고 수익률
     */
    private double highYield;
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
        if (tradeEvent == TradeEvent.SELL) {
            return buyAmount * getRealYield() - feePrice;
        }
        return 0;
    }

    /**
     * @return 투자 결과<br>
     * 투자금 + 투자 수익
     */
    public double getInvestResult() {
        return buyAmount + getGains();
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
        if (tradeEvent == TradeEvent.SELL) {
            return (askPrice / bidPrice) - 1;
        }
        return 0;
    }

    public String toString() {
        String dateKst = DateUtil.formatDateTime(candle.getCandleDateTimeKst());
        String dateUtc = DateUtil.formatDateTime(candle.getCandleDateTimeUtc());
        return String.format("날짜(KST): %s, 날짜(UTC): %s, 코인: %s, 이벤트 유형: %s, 시가: %,.0f, 고가:%,.0f, 저가:%,.0f, " +
                        "종가:%,.0f, 단기 이동평균: %,.0f, 장기 이동평균: %,.0f, " +
                        "매수 체결 가격: %,.0f, 최고수익률: %,.2f%%, 매도 체결 가격: %,.0f, 매도 이유: %s, " +
                        "실현 수익률: %,.2f%%, 매수금액: %,.0f, 전체코인 매수금액: %,.0f, 현금: %,.0f, 수수료: %,.0f, 투자 수익(수수료포함): %,.0f, " +
                        "투자 결과(수수료포함): %,.0f, 현금 + 전체코인 매수금액: %,.0f",
                dateKst, dateUtc, candle.getMarket(), tradeEvent, candle.getOpeningPrice(), candle.getHighPrice(), candle.getLowPrice(),
                candle.getTradePrice(), maShort, maLong,
                bidPrice, highYield * 100, askPrice, askReason == null ? "" : askReason,
                getRealYield() * 100, buyAmount, buyTotalAmount, cash, feePrice, getGains(),
                getInvestResult(), getFinalResult());
    }
}
