package com.setvect.bokslcoin.autotrading.algorithm;

interface TradeService {
    /**
     * 매수
     *
     * @param askPrice 투자 금액
     */
    public void bid(double askPrice);

    /**
     * 매도
     *
     * @param askType 매도 유형
     */
    public void ask(VbsStopService.AskType askType);
}
