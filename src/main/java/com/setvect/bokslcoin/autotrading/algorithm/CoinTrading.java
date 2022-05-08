package com.setvect.bokslcoin.autotrading.algorithm;

import com.setvect.bokslcoin.autotrading.algorithm.websocket.TradeResult;

/**
 * 알고리즘
 */
public interface CoinTrading {
    /**
     * 새로운 체결 이벤트 발생 시 호출
     *
     * @param tradeResult 체결 현황
     */
    void tradeEvent(TradeResult tradeResult);
}
