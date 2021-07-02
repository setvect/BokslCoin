package com.setvect.bokslcoin.autotrading.backtest.vbs;

import com.setvect.bokslcoin.autotrading.algorithm.TradePeriod;
import com.setvect.bokslcoin.autotrading.backtest.BaseCondition;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * 변동성 돌파 + 손절 + 익절 전략 변수
 */
@SuperBuilder
@Getter
@ToString
public class VbsTrailingStopCondition extends BaseCondition {
    /**
     * 변동성 돌파 판단 비율
     */
    private final double k;
    /**
     * 평단가 주기, 해당 평단가 이상인 경우 매수함, 값이 0이면 변동성 돌파만 판단함
     */
    private final int ma;

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
}
