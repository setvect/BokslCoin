package com.setvect.bokslcoin.autotrading.algorithm;

import java.time.ZonedDateTime;

/**
 * 매매시 발생하는 이벤트
 */
public interface TradeEvent {

    void newPeriod(ZonedDateTime startUtc);

    /**
     * @param market     코인
     * @param tradePrice 매수 당시 가격
     * @param bidPrice   매수 금액
     */
    void bid(String market, double tradePrice, double bidPrice);

    /**
     * @param market     코인
     * @param balance    매도 물량
     * @param tradePrice 매도 당시 가격
     * @param reason     매도 이유
     */
    void ask(String market, double balance, double tradePrice, VbsStopService.AskReason reason);

    /**
     * @param targetPrice 매수 목표가
     */
    void registerTargetPrice(double targetPrice);
}
