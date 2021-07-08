package com.setvect.bokslcoin.autotrading.backtest.mabsss;

import com.setvect.bokslcoin.autotrading.algorithm.AskReason;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 주기별 매매 정보
 */
@Setter
@Getter
public class MabsSplitSellBacktestRow {
    private Candle candle;
    // 매매 여부
    private boolean trade;
    // 매수 체결 가격
    private double bidPrice;
    // 매도 체결 가격
    private double askPrice;
    // 투자금
    // 현금
    private double cash;
    // 매매 수수료
    private double feePrice;
    private AskReason askReason;
    /**
     * 최고 수익률
     */
    private double highYield;
    /**
     * 직전 캔들 종가
     */
    private Double beforeTradePrice = Double.valueOf(0);

    /**
     * 단기 이동평균
     */
    private double maShort;

    /**
     * 장기 이동평균
     */
    private double maLong;

    /**
     * 보유 물량
     */
    private double balance;

    /**
     * 매도 물량
     */
    private double askBalance;

    private List<Double> gainsHistory = new ArrayList<>();

    public void addGainHistory(double d){
        this.gainsHistory.add(d);
    }

    public MabsSplitSellBacktestRow(Candle candle) {
        this.candle = candle;
    }

    /**
     * @return 투자금
     */
    public double getInvestmentAmount() {
        return bidPrice * balance;
    }

    /**
     * @return 투자 수익(수수료 제외)
     */

    public double getGains() {
        if (askPrice == 0) {
            return 0;
        }
        return askPrice * askBalance - bidPrice * askBalance;
    }


    public double getGainsTradeSum() {
        if (askReason == AskReason.TIME || askReason == AskReason.MA_DOWN) {
            if(askReason == AskReason.TIME){
                System.out.println();
            }
            return gainsHistory.stream().mapToDouble(p -> p).sum();
        }
        return 0;
    }

    /**
     * @return 투자 결과<br>
     * 투자금 + 투자 수익
     */
    public double getInvestResult() {
        return getInvestmentAmount() + getGains();
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
        if (askPrice == 0) {
            return 0;
        }
        if (trade) {
            return (askPrice / bidPrice) - 1;
        }
        return 0;
    }

    public String toString() {
        String dateKst = DateUtil.formatDateTime(candle.getCandleDateTimeKst());
        String dateUtc = DateUtil.formatDateTime(candle.getCandleDateTimeUtc());
        return String.format("날짜(KST): %s, 날짜(UTC): %s, 시가: %,.0f, 고가:%,.0f, 저가:%,.0f, " +
                        "종가:%,.0f, 직전 종가:%,.0f, 단위 수익률: %,.2f%%, 단기 이동평균: %,.0f, 장기 이동평균: %,.0f, 매매여부: %s, " +
                        "매수 체결 가격: %,.0f, 보유 물량: %,f, 최고수익률: %,.2f%%, 매도 체결 가격: %,.0f, 매도 물량: %,f, 매도 이유: %s, " +
                        "실현 수익률: %,.2f%%, 투자금: %,.0f, 현금: %,.0f, 투자 수익: %,.0f, 수수료: %,.0f, " +
                        "투자 결과: %,.0f, 현금 + 투자결과 - 수수료: %,.0f",
                dateKst, dateUtc, candle.getOpeningPrice(), candle.getHighPrice(), candle.getLowPrice(),
                candle.getTradePrice(), beforeTradePrice, getCandleYield() * 100, maShort, maLong, trade,
                bidPrice, balance, highYield * 100, askPrice, askBalance, askReason == null ? "" : askReason,
                getRealYield() * 100, getInvestmentAmount(), cash, getGains(), feePrice,
                getInvestResult(), getFinalResult());
    }
}
