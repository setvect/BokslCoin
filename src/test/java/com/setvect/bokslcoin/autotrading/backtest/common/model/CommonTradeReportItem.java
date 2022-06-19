package com.setvect.bokslcoin.autotrading.backtest.common.model;

import com.setvect.bokslcoin.autotrading.backtest.entity.common.CommonTradeEntity;
import lombok.Builder;
import lombok.Getter;

/**
 * 거래 건별 내역
 */
// TODO 공통 모듈화 작업 진행해야됨
@Getter
@Builder
public class CommonTradeReportItem<T extends CommonTradeEntity> {
    /**
     * 거래 수익률
     */
    private T tradeEntity;
    /**
     * 매수금액
     */
    private double buyAmount;
    /**
     * 전체 코인 매수 금액
     */
    private double buyTotalAmount;
    /**
     * 현금
     */
    private double cash;
    /**
     * 매매 수수료
     */
    private double feePrice;

    /**
     * 투자 수익(수수료 포함)
     */
    private double gains;


    /**
     * @return 투자 결과<br>
     * 투자금 + 투자 수익
     */
    public double getInvestResult() {
        return buyAmount + getGains();
    }

    /**
     * @return 현금 + 전체코인 매수금액
     */
    public double getFinalResult() {
        return getBuyTotalAmount() + cash;
    }

    /**
     * @return 실현 수익률
     */
    public double getRealYield() {
        return tradeEntity.getYield();
    }
}