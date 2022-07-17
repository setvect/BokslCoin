package com.setvect.bokslcoin.autotrading.backtest.neovbs;

import com.setvect.bokslcoin.autotrading.backtest.common.model.AnalysisMultiCondition;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.entity.neovbs.NeoVbsConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.mabs.service.MabsMakeBacktestReportService;
import com.setvect.bokslcoin.autotrading.backtest.neovbs.service.NeoVbsBacktestService;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class NeoVbsAnalyzerTest {

    @Autowired
    private MabsMakeBacktestReportService makeBacktestReportService;
    @Autowired
    private NeoVbsBacktestService neoVbsBacktestService;

    @Test
    @DisplayName("변동성 돌파 전략 백테스트")
    public void backtest() {
        LocalDateTime baseStart = DateUtil.getLocalDateTime("2022-05-01T00:00:00");
        LocalDateTime baseEnd = DateUtil.getLocalDateTime("2022-06-01T00:00:00");
        List<String> markets = Arrays.asList("KRW-XRP");
        final DateRange range = new DateRange(baseStart, baseEnd);
        AnalysisMultiCondition.AnalysisMultiConditionBuilder analysisMultiConditionBuilder = AnalysisMultiCondition.builder()
                .range(range)
                .investRatio(.99)
                .cash(14_223_714)
                .feeSell(0.002) // 슬립피지까지 고려해 보수적으로 0.2% 수수료 측정
                .feeBuy(0.002);

        neoVbsBacktestService.backtest(
                makeCondition(markets),
                range, analysisMultiConditionBuilder
        );
    }

    /**
     * @param markets 매매 코인
     * @return 기본 조건
     */
    private List<NeoVbsConditionEntity> makeCondition(List<String> markets) {
        List<NeoVbsConditionEntity> mabsConditionEntities = new ArrayList<>();
        for (String market : markets) {
            log.info("{} start", market);
            NeoVbsConditionEntity condition = NeoVbsConditionEntity.builder()
                    .market(market)
                    .tradePeriod(PeriodType.PERIOD_1440)
                    .k(0.5)
                    .loseStopRate(0.5)
                    .trailingLossStopRate(0.1)
                    .trailingStopEnterRate(0.1)
                    .comment(null)
                    .build();
            mabsConditionEntities.add(condition);
        }
        return mabsConditionEntities;
    }
}
