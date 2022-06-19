package com.setvect.bokslcoin.autotrading.backtest.mabs;

import com.setvect.bokslcoin.autotrading.backtest.common.AnalysisMultiCondition;
import com.setvect.bokslcoin.autotrading.backtest.common.CommonAnalysisReportResult;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.mabs.service.MabsBacktestService;
import com.setvect.bokslcoin.autotrading.backtest.mabs.service.MakeBacktestReportService;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class MabsTradeAnalyzerTest {
    @Autowired
    private MakeBacktestReportService makeBacktestReportService;
    @Autowired
    private MabsBacktestService mabsBacktestService;

    @Test
    @DisplayName("변동성 돌파 전략 백테스트")
    public void backtest() {
//        LocalDateTime baseStart = backtestHelperService.makeBaseStart(market, PeriodType.PERIOD_60, period.getRight() + 1);
//        LocalDateTime baseStart = DateUtil.getLocalDateTime("2022-01-10T00:00:00");
//        LocalDateTime baseEnd = DateUtil.getLocalDateTime("2022-06-02T23:59:59");
        LocalDateTime baseStart = DateUtil.getLocalDateTime("2022-05-01T00:00:00");
        LocalDateTime baseEnd = DateUtil.getLocalDateTime("2022-06-01T00:00:00");
//        LocalDateTime baseStart = DateUtil.getLocalDateTime("2022-01-10T00:00:00");
//        LocalDateTime baseEnd = LocalDateTime.now();

        List<String> markets = Arrays.asList("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-EOS", "KRW-ETC", "KRW-ADA", "KRW-MANA", "KRW-BAT", "KRW-BCH", "KRW-DOT");
//        List<String> markets = Arrays.asList("KRW-BTC");
        final DateRange range = new DateRange(baseStart, baseEnd);

        AnalysisMultiCondition.AnalysisMultiConditionBuilder analysisMultiConditionBuilder = AnalysisMultiCondition.builder()
                .range(range)
                .investRatio(.99)
                .cash(14_223_714)
                .feeSell(0.002) // 슬립피지까지 고려해 보수적으로 0.2% 수수료 측정
                .feeBuy(0.002);

        mabsBacktestService.backtest(
                makeCondition(markets),
                range, analysisMultiConditionBuilder
        );
    }

    @Test
    @DisplayName("기 매매 결과에서 추가된 시세 데이터에 대한 증분 수행")
    public void backtestIncremental() {
        List<Integer> conditionSeqList = Arrays.asList(
                27288611, // KRW-BTC(2017-10-16)
                27346706, // KRW-ETH(2017-10-10)
                27403421, // KRW-XRP(2017-10-10)
                27458175, // KRW-EOS(2018-03-30)
                27508376, // KRW-ETC(2017-10-09)
                29794493, // KRW-ADA(2017-10-16)
                36879612, // KRW-MANA(2019-04-09)
                36915333, // KRW-BAT(2018-07-30)
                44399001, // KRW-BCH(2017-10-08)
                44544109  // KRW-DOT(2020-10-15)
        );

        mabsBacktestService.backtestIncremental(conditionSeqList);
    }

    @Test
    @Transactional
    @DisplayName("기 분석된 매매 내역을 조합해 백테스트 리포트 만듦")
    public void makeBacktestReport() {
        // 60, 13, 64
        List<Integer> conditionSeqList = Arrays.asList(
                27288611,// KRW-BTC(2017-10-16)
                27346706,// KRW-ETH(2017-10-10)
                27403421,// KRW-XRP(2017-10-10)
                27458175,// KRW-EOS(2018-03-30)
                27508376,// KRW-ETC(2017-10-09)
                29794493,// KRW-ADA(2017-10-16)
                36879612,// KRW-MANA(2019-04-09)
                36915333,// KRW-BAT(2018-07-30)
                44399001,// KRW-BCH(2017-10-08)
                44544109//  KRW-DOT(2020-10-15)
        );
//         60, 1, 64 +- 0.03%
//        List<Integer> conditionSeqList = Arrays.asList(
//                45962518,// KRW-BTC(2017-10-16)
//                46019888,// KRW-ETH(2017-10-10)
//                46077434,// KRW-XRP(2017-10-10)
//                46134607,// KRW-EOS(2018-03-30)
//                46185678,// KRW-ETC(2017-10-09)
//                46242164,// KRW-ADA(2017-10-16)
//                46299027,// KRW-MANA(2019-04-09)
//                46336499,// KRW-BAT(2018-07-30)
//                46382919,// KRW-BCH(2017-10-08)
//                46440134//  KRW-DOT(2020-10-15)
//        );

        //30, 24, 90
//        List<Integer> conditionSeqList = Arrays.asList(
//                44667780,// KRW-BTC(2017-10-16)
//                44783586,// KRW-ETH(2017-10-10)
//                44899267,// KRW-XRP(2017-10-10)
//                45008726,// KRW-EOS(2018-03-30)
//                45109548,// KRW-ETC(2017-10-09)
//                45219094,// KRW-ADA(2017-10-16)
//                45330679,// KRW-MANA(2019-04-09)
//                45400419,// KRW-BAT(2018-07-30)
//                45488562,// KRW-BCH(2017-10-08)
//                45598284//  KRW-DOT(2020-10-15)
//        );
//        List<DateRange> rangeList = Arrays.asList(
//                new DateRange("2020-11-01T00:00:00", "2021-04-14T23:59:59"), // 상승장
//                new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59"), // 상승장 후 하락장
//                new DateRange("2020-05-07T00:00:00", "2020-10-20T23:59:59"), // 횡보장1
//                new DateRange("2020-05-08T00:00:00", "2020-07-26T23:59:59"), // 횡보장2
//                new DateRange("2019-06-24T00:00:00", "2020-03-31T23:59:59"), // 횡보+하락장1
//                new DateRange("2017-12-24T00:00:00", "2020-03-31T23:59:59"), // 횡보+하락장2
//                new DateRange("2018-01-01T00:00:00", "2020-11-19T23:59:59"), // 횡보장3
//                new DateRange("2021-04-14T00:00:00", "2021-06-08T23:59:59"), // 하락장1
//                new DateRange("2017-12-07T00:00:00", "2018-02-06T23:59:59"), // 하락장2
//                new DateRange("2018-01-06T00:00:00", "2018-02-06T23:59:59"), // 하락장3
//                new DateRange("2018-01-06T00:00:00", "2018-12-15T23:59:59"), // 하락장4(찐하락장)
//                new DateRange("2019-06-27T00:00:00", "2020-03-17T23:59:59"), // 하락장5
//                new DateRange("2018-01-06T00:00:00", "2019-08-15T23:59:59"), // 하락장 이후 약간의 상승장
//                new DateRange("2021-06-14T00:00:00", "2022-12-31T23:59:59"), // 최근
//                new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59"), // 전체 기간1
//                new DateRange("2017-10-01T00:00:00", "2022-12-31T23:59:59")  // 전체 기간2
//        );

        List<DateRange> rangeList = Arrays.asList(
//                new DateRange(DateUtil.getLocalDateTime("2022-01-10T00:00:00"), LocalDateTime.now())
//                new DateRange(DateUtil.getLocalDateTime("2021-06-08T00:00:00"), LocalDateTime.now())
                new DateRange(DateUtil.getLocalDateTime("2017-12-31T23:59:59"), LocalDateTime.now())

//                new DateRange("2017-10-01T00:00:00", "2017-12-31T23:59:59"),
//                new DateRange("2018-01-01T00:00:00", "2018-06-30T23:59:59"),
//                new DateRange("2018-07-01T00:00:00", "2018-12-31T23:59:59"),
//                new DateRange("2019-01-01T00:00:00", "2019-06-30T23:59:59"),
//                new DateRange("2019-07-01T00:00:00", "2019-12-31T23:59:59"),
//                new DateRange("2020-01-01T00:00:00", "2020-06-30T23:59:59"),
//                new DateRange("2020-07-01T00:00:00", "2020-12-31T23:59:59"),
//                new DateRange("2021-01-01T00:00:00", "2021-06-30T23:59:59"),
//                new DateRange("2021-07-01T00:00:00", "2021-12-31T23:59:59"),
//                new DateRange("2018-01-01T00:00:00", "2018-12-31T23:59:59"),
//                new DateRange("2019-01-01T00:00:00", "2019-12-31T23:59:59"),
//                new DateRange("2020-01-01T00:00:00", "2020-12-31T23:59:59"),
//                new DateRange("2021-01-01T00:00:00", "2021-12-31T23:59:59")
        );

        List<CommonAnalysisReportResult> accResult = new ArrayList<>();
        int count = 0;
        int total = rangeList.size() * conditionSeqList.size();
//        for (Integer conditionSeq : conditionSeqList) {
        for (DateRange dateRange : rangeList) {
            AnalysisMultiCondition analysisMultiCondition = AnalysisMultiCondition.builder()
                    .conditionIdSet(new HashSet<>(conditionSeqList))
                    .range(dateRange)
                    .investRatio(.99)
//                    .cash(15_000_000)
                    .cash(14_223_714)
//                    .cash(10_000_000)
                    .feeSell(0.002) // 슬립피지까지 고려해 보수적으로 0.2% 수수료 측정
                    .feeBuy(0.002)
                    .build();

            CommonAnalysisReportResult result = makeBacktestReportService.makeReport(analysisMultiCondition);
            accResult.add(result);
            count++;

            log.info("{}/{} - {}", count, total, dateRange);
        }
//        }

        makeBacktestReportService.makeReportMulti(accResult);
        System.out.println("끝");
    }


    /**
     * @param markets 매매 코인
     * @return 기본 조건
     */
    private List<MabsConditionEntity> makeCondition(List<String> markets) {

        List<Pair<Integer, Integer>> periodList = new ArrayList<>();
        periodList.add(new ImmutablePair<>(13, 64));

        List<MabsConditionEntity> mabsConditionEntities = new ArrayList<>();
        for (Pair<Integer, Integer> period : periodList) {
            for (String market : markets) {
                log.info("{} - {} start", period, market);
                MabsConditionEntity condition = MabsConditionEntity.builder()
                        .market(market)
                        .tradePeriod(PeriodType.PERIOD_60)
                        .upBuyRate(0.01)
                        .downSellRate(0.01)
                        .shortPeriod(period.getLeft())
                        .longPeriod(period.getRight())
                        .loseStopRate(0.5)
                        .comment(null)
                        .build();
                mabsConditionEntities.add(condition);
            }
        }
        return mabsConditionEntities;
    }
}
