package com.setvect.bokslcoin.autotrading.backtest.common;

import com.setvect.bokslcoin.autotrading.util.DateRange;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

/**
 * 매매 분석 조건
 */
@Getter
@Builder
public class AnalysisMultiCondition {
    /**
     * 매매 조건
     */
    private final Set<Integer> conditionIdSet;
    /**
     * 분석 대상 기간(KST)
     */
    private final DateRange range;
    /**
     * 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
     */
    private final double investRatio;
    /**
     * 최초 투자 금액
     */
    private final double cash;
    /**
     * 매수 수수료
     */
    private final double feeBuy;
    /**
     * 매도 수수료
     */
    private final double feeSell;
    /**
     * 조건에 대한 설명. 리포트에서 사용하기 위함
     */
    private final String comment;
}
