package com.setvect.bokslcoin.autotrading.backtest.entity.common;

import com.setvect.bokslcoin.autotrading.record.entity.TradeType;

import java.time.LocalDateTime;

/**
 * 백테스트 매매 건별 정보
 */
public interface CommonTradeEntity {
    /**
     * 매수/매도
     */
    TradeType getTradeType();

    /**
     * 매도시 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     * 매수는 0으로 표현
     */
    double getYield();

    /**
     * 거래 단가
     * - 매수 일경우 매수 단가
     * - 매도 일경우 매도 단가
     */
    double getUnitPrice();

    /**
     * 거래시간(KST 기준)
     */
    LocalDateTime getTradeTimeKst();

    /**
     * 매매 조건 일련번호
     */
    ConditionEntity getConditionEntity();
}
