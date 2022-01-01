package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis;

import com.setvect.bokslcoin.autotrading.backtest.entity.MabsConditionEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 멀티 코인 매매 백테스트 분석 결과
 */
@Getter
@Builder
public class AnalysisReportResult {
    /**
     * 리포트 조건
     */
    private AnalysisMultiCondition condition;
    /**
     * 종목별 매매 조건
     */
    private List<MabsConditionEntity> conditionList;
    /**
     * 매매 이력
     */
    private List<MabsTradeReportItem> tradeHistory;
    /**
     * 전체 수익 결과
     */
    private TotalYield totalYield;

    /**
     * 코인별 승률
     */
    private Map<String, WinningRate> coinWinningRate;

    /**
     * 코인별 Buy&Hold 수익률
     */
    private MultiCoinHoldYield multiCoinHoldYield;

    /**
     * @return 코인 이름
     */
    public Set<String> getMarkets() {
        return conditionList.stream().map(MabsConditionEntity::getMarket).collect(Collectors.toSet());
    }

}
