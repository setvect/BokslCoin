package com.setvect.bokslcoin.autotrading.backtest;

import com.setvect.bokslcoin.autotrading.algorithm.TradePeriod;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@ToString
@SuperBuilder
public class BaseCondition {
    /**
     * 분석 대상 기간(UTC)
     */
    private final DateRange range;
    /**
     * 대상 코인
     */
    private final String market;
    /**
     * 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
     */
    private final double investRatio;
    /**
     * 최초 투자 금액
     */
    private final double cash;
    /**
     * 매매시 채결 가격 차이
     * 시장가로 매매하기 때문에 한단계 낮거나 높은 호가로 매매가 되는 것을 고려함.
     * 매수 채결 가격 = 목표가격 + tradeMargin
     * 매도 채결 가격 = 종가 - tradeMargin
     */
    private final double tradeMargin;
    /**
     * 매수 수수료
     */
    private final double feeBid;
    /**
     * 매도 수수료
     */
    private final double feeAsk;

    /**
     * 매매 주기
     */
    private final TradePeriod tradePeriod;

    /**
     * 손절 손실율<br>
     * 예를 들어 0.05이면 수익율이 -5%가 되면 손절 매도
     */
    private final double loseStopRate;

    /**
     * 조건에 대한 설명. 리포트에서 사용하기 위함
     */
    private final String comment;

}
