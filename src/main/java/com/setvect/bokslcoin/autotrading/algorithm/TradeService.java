package com.setvect.bokslcoin.autotrading.algorithm;

public interface TradeService {
    /**
     * 매수
     *
     * @param investment 투자 금액
     * @param cash       현금
     */
    void bid(double investment, double cash);

    /**
     * 매도
     *
     * @param tradePrice 매도 가격
     * @param askType    매도 유형
     */
    void ask(Double tradePrice, VbsStopService.AskType askType);

    /**
     * * @param targetValue 매수 목표가
     */
    void applyTargetPrice(double targetValue);
}
