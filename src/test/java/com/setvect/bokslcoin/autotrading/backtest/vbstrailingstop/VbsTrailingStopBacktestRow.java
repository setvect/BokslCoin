package com.setvect.bokslcoin.autotrading.backtest.vbstrailingstop;

import com.setvect.bokslcoin.autotrading.algorithm.AskReason;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * 날짜별 매매 정보
 */
@Setter
@Getter
public class VbsTrailingStopBacktestRow {
    private Candle candle;
    // 목표가
    private double targetPrice;
    // 매매 여부
    private boolean trade;
    // 매수 체결 가격
    private double bidPrice;
    // 매도 체결 가격
    private double askPrice;
    // 투자금
    private double investmentAmount;
    // 현금
    private double cash;
    // 매매 수수료
    private double feePrice;
    private AskReason askReason;
    /**
     * 트레일링 스탑 진입 여부
     */
    private boolean trailingTrigger;
    /**
     * 최고 수익률
     */
    private double highYield;

    /**
     * 직전 캔들 종가
     */
    private Double beforeTradePrice = Double.valueOf(0);

    public VbsTrailingStopBacktestRow(Candle candle) {
        this.candle = candle;
    }

    // 투자 수익
    public double getGains() {
        return investmentAmount * getRealYield();
    }

    /**
     * @return 투자 결과<br>
     * 투자금 + 투자 수익
     */
    public double getInvestResult() {
        return investmentAmount + getGains();
    }

    /**
     * @return 현금 + 투자 결과
     * 투자금 + 투자 수익 - 수수료
     */
    public double getFinalResult() {
        return getInvestResult() + cash - feePrice;
    }


    /**
     * 직전 캔들 종가에 매수 해서 종가에 매도 했을 때 얻는 수익률
     *
     * @return 캔들상 수익률<br>
     */
    public double getCandleYield() {
        double diff = candle.getTradePrice() - beforeTradePrice;
        return diff / beforeTradePrice;
    }

    /**
     * @return 실현 수익률
     */
    public double getRealYield() {
        if (trade) {
            return (askPrice / bidPrice) - 1;
        }
        return 0;
    }

    public String toString() {
        String dateKst = DateUtil.formatDateTime(candle.getCandleDateTimeKst());
        String dateUtc = DateUtil.formatDateTime(candle.getCandleDateTimeUtc());
        return String.format("날짜(KST): %s, 날짜(UTC): %s, 시가: %,.0f, 고가:%,.0f, 저가:%,.0f, " +
                        "종가:%,.0f, 직전 종가:%,.0f, 단위 수익률: %,.2f%%, 매수 목표가: %,.0f, 매매여부: %s, " +
                        "매수 체결 가격: %,.0f, 트레일링 스탑 진입: %s, 최고수익률: %,.2f%%, 매도 체결 가격: %,.0f, 매도 이유: %s, " +
                        "실현 수익률: %,.2f%%, 투자금: %,.0f, 현금: %,.0f, 투자 수익: %,.0f, 수수료: %,.0f, " +
                        "투자 결과: %,.0f, 현금 + 투자결과 - 수수료: %,.0f",
                dateKst, dateUtc, candle.getOpeningPrice(), candle.getHighPrice(), candle.getLowPrice(),
                candle.getTradePrice(), beforeTradePrice, getCandleYield() * 100, targetPrice, trade,
                bidPrice, trailingTrigger, highYield * 100, askPrice, askReason == null ? "" : askReason,
                getRealYield() * 100, investmentAmount, cash, getGains(), feePrice,
                getInvestResult(), getFinalResult());
    }
}
