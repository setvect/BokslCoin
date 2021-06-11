package com.setvect.bokslcoin.autotrading.algorithm;

/**
 * 매매시 발생하는 이벤트
 */
public interface TradeEvent {
    /**
     * @param market
     * @param currentPrice
     * @param askPrice
     */
    void bid(String market, double currentPrice, double askPrice);

    /**
     * @param market
     * @param balance      매도 물량
     * @param currentPrice 매도 당시 가격
     * @param reason       매도 이유
     */
    void ask(String market, double balance, double currentPrice, VbsStopService.AskReason reason);

    /**
     * @param targetPrice 매수 목표가
     */
    void registerTargetPrice(double targetPrice);
}
