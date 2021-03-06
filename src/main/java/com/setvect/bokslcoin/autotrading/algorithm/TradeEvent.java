package com.setvect.bokslcoin.autotrading.algorithm;

import com.setvect.bokslcoin.autotrading.algorithm.websocket.TradeResult;
import com.setvect.bokslcoin.autotrading.model.Candle;

/**
 * 매매시 발생하는 이벤트
 */
public interface TradeEvent {

    /**
     * 새로운 분석 주기
     *
     * @param candle 분봉
     */
    void newPeriod(TradeResult candle);

    /**
     * 시세 체크
     *
     * @param candle 분봉
     */
    void check(Candle candle);

    /**
     * 시세 체크
     *
     * @param ma 이동평균
     */
    void setMaPrice(double ma);

    /**
     * 이동평균 체크
     *
     * @param candle  현재 시세
     * @param maShort 단기 이동평균
     * @param maLong  장기 이동평균
     */
    void check(Candle candle, double maShort, double maLong);

    /**
     * 이동평균 체크
     *
     * @param candle    분봉
     * @param currentMa 단기 이동평균
     * @param maxMa     직전 이동 평균에서 연속된 최대값
     * @param minMa     직전 이동 평균에서 연속된 최소값
     */
    void check(Candle candle, double currentMa, double maxMa, double minMa);

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
    void ask(String market, double balance, double tradePrice, AskReason reason);

    /**
     * @param market    코인
     * @param highYield 최고 수익률
     */
    void highYield(String market, double highYield);

    /**
     * @param market   코인
     * @param lowYield 최저 수익률
     */
    void lowYield(String market, double lowYield);

    /**
     * @param targetPrice 매수 목표가
     */
    void registerTargetPrice(String market, double targetPrice);


}
