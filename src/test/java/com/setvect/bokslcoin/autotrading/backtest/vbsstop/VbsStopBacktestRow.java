package com.setvect.bokslcoin.autotrading.backtest.vbs;

import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * 날짜별 매매 정보
 */
@Setter
@Getter
public class VbsStopBacktestRow {
    private CandleDay candleDay;
    // 목표가1
    private double targetPrice;
    // 매매 여부
    private boolean trade;
    // 매수 체결 가격
    private double bidPrice;
    // 매도 체결 가격
    private double askPrice;
    // 투자금
    private double invest;
    // 현금
    private double cash;


    // 매매 수수료
    private double feePrice;
    // 코인
    private double coin;

    public VbsStopBacktestRow(CandleDay candle) {
        this.candleDay = candle;
    }

    // 투자 수익
    public double getGains() {
        return invest * getRealYield();
    }

    /**
     * @return 투자 결과<br>
     * 투자금 + 투자 수익
     */
    public double getInvestResult() {
        return invest + getGains();
    }

    /**
     * @return 현금 + 투자 결과
     * 투자금 + 투자 수익 - 수수료
     */
    public double getFinalResult() {
        return getInvestResult() + cash - feePrice;
    }


    /**
     * 시가에 매도 해서 종가에 팔았을 때 얻는 수익률
     *
     * @return 캔들상 수익률<br>
     */
    public double getCandleYield() {
        return (candleDay.getTradePrice() / candleDay.getOpeningPrice()) - 1;
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
        String date = DateUtil.formatDateTime(candleDay.getCandleDateTimeUtc());
        return String.format("날짜: %s, 시가: %,.0f, 고가:%,.0f, 저가:%,.0f, 종가:%,.0f, 단위 수익률: %,.2f%%, 매수 목표가: %,.0f, 매매여부: %s, 매수 체결 가격: %,.0f, 매도 체결 가격: %,.0f, 실현 수익률: %,.2f%%, 투자금: %,.0f, 현금: %,.0f, 투자 수익: %,.0f, 수수료: %,.0f, 투자 결과: %,.0f, 현금 + 투자결과 - 수수료: %,.0f",
                date, candleDay.getOpeningPrice()
                , candleDay.getHighPrice(),
                candleDay.getLowPrice(), candleDay.getTradePrice(), getCandleYield() * 100,
                targetPrice, trade, bidPrice, askPrice, getRealYield() * 100, invest, cash, getGains(), feePrice, getInvestResult(), getFinalResult());
    }
}
