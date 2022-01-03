package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis;

import com.setvect.bokslcoin.autotrading.algorithm.TradePeriod;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@ToString
@SuperBuilder
public class AnalysisCoinCondition {
    /**
     * 분석 대상 기간(UTC)
     */
    private final DateRange range;
    /**
     * 대상 코인
     */
    private final String market;
    /**
     * 매매 주기
     */
    private final TradePeriod tradePeriod;

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
    /**
     * 손절 손실률<br>
     * 예를 들어 0.05이면 수익률이 -5%가 되면 손절 매도
     */
    private final double loseStopRate;

    /**
     * 조건에 대한 설명. 리포트에서 사용하기 위함
     */
    private final String comment;
}
