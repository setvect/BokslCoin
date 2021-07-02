package com.setvect.bokslcoin.autotrading.backtest.vbs;

import com.setvect.bokslcoin.autotrading.algorithm.TradePeriod;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 변동성 돌파 + 손절 + 익절 전략 변수
 */
@Builder
@Getter
@ToString
public class VbsTrailingStopCondition {
    // 변동성 돌파 판단 비율
    private final double k;
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
     * 손절 손실율<br>
     * 예를 들어 0.05이면 수익율이 -5%가 되면 손절 매도
     */
    private final double loseStopRate;

    /**
     * 익절 수익율<br>
     * 예를 들어 0.1이면 수익율이 10%가 되면 익절 매도
     */
    private final double gainStopRate;
    /**
     * gainStopRate 이상 상승 후 전고점 대비 trailingStopRate 비율 만큼 하락하면 시장가 매도
     * 예를 들어 trailingStopRate 값이 0.02일 때 고점 수익률이 12%인 상태에서 10%미만으로 떨어지면 시장가 매도
     */
    private final double trailingStopRate;

    private final TradePeriod tradePeriod;
    /**
     * 조건에 대한 설명. 리포트에서 사용하기 위함
     */
    private final String comment;
}
