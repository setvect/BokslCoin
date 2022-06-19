package com.setvect.bokslcoin.autotrading.backtest.common;

import com.setvect.bokslcoin.autotrading.backtest.common.model.CommonAnalysisReportResult;
import com.setvect.bokslcoin.autotrading.backtest.entity.CandleEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepositoryCustom;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BacktestHelperComponent {
    @Autowired
    private CandleRepositoryCustom candleRepositoryCustom;

    /**
     * @param market 마켓
     * @param period 주기
     *               -0-0 * @param n      .
     * @return 최초 시세 기준에서 n번째 candle의 UTC 시간
     */
    public LocalDateTime makeBaseStart(String market, PeriodType period, int n) {
        LocalDateTime base = LocalDateTime.of(2000, 1, 1, 0, 0);
        List<CandleEntity> candleList = candleRepositoryCustom.findMarketPricePeriodAfter(market, period, base, PageRequest.of(0, n));
        if (candleList.size() != n) {
            throw new RuntimeException(String.format("시세 데이터 부족, 기대값: %d, 실제값: %d", n, candleList.size()));
        }
        return candleList.get(n - 1).getCandleDateTimeUtc();
    }


    /**
     * 기간동안 보유(존버)할 경우 수익률 계산
     *
     * @param range   투자 기간
     * @param markets 대상 코인
     * @return 기간별 코인 수익률
     */
    public CommonAnalysisReportResult.MultiCoinHoldYield calculateCoinHoldYield(DateRange range, Set<String> markets) {
        Map<String, List<CandleEntity>> coinCandleListMap = markets.stream()
                .collect(Collectors.toMap(Function.identity(),
                        p -> candleRepositoryCustom.findMarketPrice(p, PeriodType.PERIOD_1440, range.getFrom(), range.getTo()))
                );

        Map<String, CommonAnalysisReportResult.YieldMdd> coinByYield = BacktestHelper.getCoinByYield(coinCandleListMap);
        CommonAnalysisReportResult.YieldMdd sumYield = BacktestHelper.getYieldMdd(range, coinCandleListMap);

        return CommonAnalysisReportResult.MultiCoinHoldYield.builder()
                .coinByYield(coinByYield)
                .sumYield(sumYield)
                .build();
    }
}
