package com.setvect.bokslcoin.autotrading.algorithm;

import com.setvect.bokslcoin.autotrading.algorithm.websocket.TradeResult;

/**
 * 알고리즘
 */
public interface CoinTrading {
    void tradeEvent(TradeResult tradeResult);
}
