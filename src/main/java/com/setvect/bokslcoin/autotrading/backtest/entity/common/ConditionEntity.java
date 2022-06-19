package com.setvect.bokslcoin.autotrading.backtest.entity.common;

/**
 * 백테스트 기본 조건
 */
public interface ConditionEntity {
    int getConditionSeq();

    /**
     * 코인 이름
     * KRW-BTC, KRW-ETH, ...
     */
    String getMarket();
}
