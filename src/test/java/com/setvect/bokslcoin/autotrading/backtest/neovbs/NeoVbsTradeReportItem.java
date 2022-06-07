package com.setvect.bokslcoin.autotrading.backtest.neovbs;

import com.setvect.bokslcoin.autotrading.backtest.entity.NeoVbsTradeEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NeoVbsTradeReportItem {
    /**
     * 거래 수익률
     */
    private NeoVbsTradeEntity neoVbsTradeEntity;
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
        return neoVbsTradeEntity.getYield();
    }
}