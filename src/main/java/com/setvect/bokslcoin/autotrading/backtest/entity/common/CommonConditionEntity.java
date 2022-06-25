package com.setvect.bokslcoin.autotrading.backtest.entity.common;

import java.util.List;

/**
 * 백테스트 기본 조건
 */
public interface CommonConditionEntity {
    int getConditionSeq();

    /**
     * 코인 이름
     * KRW-BTC, KRW-ETH, ...
     */
    String getMarket();


    <T extends CommonTradeEntity> List<T> getTradeEntityList();
}
