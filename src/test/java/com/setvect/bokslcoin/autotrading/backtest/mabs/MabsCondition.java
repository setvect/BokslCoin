package com.setvect.bokslcoin.autotrading.backtest.mabs;

import com.setvect.bokslcoin.autotrading.algorithm.TradePeriod;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 이동평균 돌파 전략 조건
 */
@Builder
@Getter
@ToString
public class MabsCondition {
    // 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
    private final double investRatio;
    // 분석 대상 기간(UTC)
    private final DateRange range;
    // 대상 코인
    private final String market;
    // 최초 투자 금액
    private final double cash;
    // 매매시 채결 가격 차이
    // 시장가로 매매하기 때문에 한단계 낮거나 높은 호가로 매매가 되는 것을 고려함.
    // 매수 채결 가격 = 목표가격 + tradeMargin
    // 매도 채결 가격 = 종가 - tradeMargin
    private final double tradeMargin;
    //  매수 수수료
    private final double feeBid;
    //  매도 수수료
    private final double feeAsk;

    /**
     * 상승 매수률
     */
    private double upBuyRate;

    /**
     * 하락 매도률
     */
    private double downSellRate;

    /**
     * 단기 이동평균 기간
     */
    private int shortPeriod;

    /**
     * 장기 이동평균 기간
     */
    private int longPeriod;


    private final TradePeriod tradePeriod;

    /**
     * 조건에 대한 설명. 리포트에서 사용하기 위함
     */
    private final String comment;
}
