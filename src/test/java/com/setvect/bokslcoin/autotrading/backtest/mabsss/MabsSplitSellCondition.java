package com.setvect.bokslcoin.autotrading.backtest.mabsss;

import com.setvect.bokslcoin.autotrading.backtest.BaseCondition;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * 이동평균 돌파 전략 조건
 */
@SuperBuilder
@Getter
@ToString
public class MabsSplitSellCondition extends BaseCondition {
    /**
     * 상승 매수률
     */
    private final double upBuyRate;

    /**
     * 하락 매도률
     */
    private final double downSellRate;

    /**
     * 단기 이동평균 기간
     */
    private final int shortPeriod;

    /**
     * 장기 이동평균 기간
     */
    private final int longPeriod;
}
