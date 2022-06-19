package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis;

import com.setvect.bokslcoin.autotrading.algorithm.common.TradeCommonService;
import com.setvect.bokslcoin.autotrading.algorithm.mabs.MabsMultiProperties;
import com.setvect.bokslcoin.autotrading.algorithm.websocket.TradeResult;
import com.setvect.bokslcoin.autotrading.backtest.common.AnalysisMultiCondition;
import com.setvect.bokslcoin.autotrading.backtest.common.BacktestHelperComponent;
import com.setvect.bokslcoin.autotrading.backtest.common.CandleDataProvider;
import com.setvect.bokslcoin.autotrading.backtest.common.mock.*;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsTradeEntity;
import com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.mock.MockMabsMultiProperties;
import com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.mock.MockMabsMultiService;
import com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.mock.MockMabsTradeEvent;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepositoryCustom;
import com.setvect.bokslcoin.autotrading.backtest.repository.MabsConditionEntityRepository;
import com.setvect.bokslcoin.autotrading.backtest.repository.MabsTradeEntityRepository;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import com.setvect.bokslcoin.autotrading.record.repository.AssetHistoryRepository;
import com.setvect.bokslcoin.autotrading.record.repository.TradeRepository;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class MabsTradeAnalyzerTest {
    /**
     * 투자금
     */
    public static final double CASH = 10_000_000;
    private final SlackMessageService slackMessageService = new MockSlackMessageService();
    private final MockMabsTradeEvent tradeEvent = new MockMabsTradeEvent(slackMessageService);
    private final MockAccountService accountService = new MockAccountService();
    private final MockOrderService orderService = new MockOrderService(new MockAccessTokenMaker(), new MockConnectionInfo());
    private final MockCandleService candleService = new MockCandleService(new MockConnectionInfo());
    @Autowired
    private MabsConditionEntityRepository mabsConditionEntityRepository;
    @Autowired
    private MabsTradeEntityRepository mabsTradeEntityRepository;
    @Autowired
    private CandleRepositoryCustom candleRepositoryCustom;
    @Autowired
    private AssetHistoryRepository assetHistoryRepository;
    @Autowired
    private TradeRepository tradeRepository;
    @Autowired
    private BacktestHelperComponent backtestHelperService;
    @Autowired
    private MakeBacktestReportService makeBacktestReportService;

    private TradeCommonService tradeCommonService;
    private MockMabsMultiService mabsMultiService;

    @Test
    @DisplayName("변동성 돌파 전략 백테스트")
    public void backtest() {
        boolean saveDb = false;
        tradeCommonService = new MockTradeCommonService(tradeEvent, accountService, orderService, candleService, tradeRepository, slackMessageService, assetHistoryRepository);
        mabsMultiService = new MockMabsMultiService(tradeCommonService, tradeEvent, new MockMabsMultiProperties());

        List<MabsConditionEntity> mabsConditionEntities = makeCondition();
//        LocalDateTime baseStart = backtestHelperService.makeBaseStart(market, PeriodType.PERIOD_60, period.getRight() + 1);
        LocalDateTime baseStart = DateUtil.getLocalDateTime("2022-01-10T00:00:00");
        LocalDateTime baseEnd = DateUtil.getLocalDateTime("2022-06-02T23:59:59");
//        LocalDateTime baseStart = DateUtil.getLocalDateTime("2022-05-01T00:00:00");
//        LocalDateTime baseEnd = DateUtil.getLocalDateTime("2022-06-01T00:00:00");

        DateRange range = new DateRange(baseStart, baseEnd);

        try {
            mabsConditionEntities = backtestSub(mabsConditionEntities, range);
            List<Integer> conditionSeqList = getConditionSeqList(mabsConditionEntities);

            AnalysisMultiCondition analysisMultiCondition = AnalysisMultiCondition.builder()
                    .conditionIdSet(new HashSet<>(conditionSeqList))
                    .range(range)
                    .investRatio(.99)
                    .cash(14_223_714)
                    .feeSell(0.002) // 슬립피지까지 고려해 보수적으로 0.2% 수수료 측정
                    .feeBuy(0.002)
                    .build();

            makeBacktestReportService.makeReport(analysisMultiCondition);
        } finally {
            if (!saveDb && CollectionUtils.isNotEmpty(mabsConditionEntities)) {
                List<Integer> conditionSeqList = getConditionSeqList(mabsConditionEntities);
                // 결과를 삭제함
                // TODO 너무 무식한 방법이다. @Transactional를 사용해야 되는데 사용하면 속도가 매우 느리다. 해결해야됨
                mabsTradeEntityRepository.deleteTradeByConditionId(conditionSeqList);
                mabsConditionEntityRepository.deleteAll(mabsConditionEntities);
            }
        }
    }

    @Test
    @DisplayName("기 매매 결과에서 추가된 시세 데이터에 대한 증분 수행")
    public void backtestIncremental() {
        tradeCommonService = new MockTradeCommonService(tradeEvent, accountService, orderService, candleService, tradeRepository, slackMessageService, assetHistoryRepository);
        mabsMultiService = new MockMabsMultiService(tradeCommonService, tradeEvent, new MockMabsMultiProperties());

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

        // 완전한 거래(매수-매도 쌍)를 만들기 위해 마지막 거래가 매수인경우 거래 내역 삭제
        deleteLastBuy(conditionSeqList);

        List<MabsConditionEntity> conditionEntityList = mabsConditionEntityRepository.findAllById(conditionSeqList);

        for (MabsConditionEntity condition : conditionEntityList) {
            log.info("{}, {}, {}_{} 시작", condition.getMarket(), condition.getTradePeriod(), condition.getLongPeriod(), condition.getShortPeriod());
            List<MabsTradeEntity> tradeList = mabsTradeEntityRepository.findByCondition(condition.getConditionSeq());

            LocalDateTime start = backtestHelperService.makeBaseStart(condition.getMarket(), condition.getTradePeriod(), condition.getLongPeriod() + 1);
            if (!tradeList.isEmpty()) {
                MabsTradeEntity lastTrade = tradeList.get(tradeList.size() - 1);
                checkLastSell(lastTrade);
                start = lastTrade.getTradeTimeKst();
            }
            DateRange range = new DateRange(start, LocalDateTime.now());
            List<MabsMultiBacktestRow> tradeHistory = backtestSub(condition, range);

            List<MabsTradeEntity> mabsTradeEntities = convert(condition, tradeHistory);
            log.info("[{}] save. range: {}, trade Count: {}", condition.getMarket(), range, mabsTradeEntities.size());
            mabsTradeEntityRepository.saveAll(mabsTradeEntities);
        }
        log.info("끝.");
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
                new DateRange(DateUtil.getLocalDateTime("2022-01-10T00:00:00"), LocalDateTime.now())
//                new DateRange(DateUtil.getLocalDateTime("2021-06-08T00:00:00"), LocalDateTime.now())

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

        List<AnalysisReportResult> accResult = new ArrayList<>();
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

            AnalysisReportResult result = makeBacktestReportService.makeReport(analysisMultiCondition);
            accResult.add(result);
            count++;

            log.info("{}/{} - {}", count, total, dateRange);
        }
//        }

        makeBacktestReportService.makeReportMulti(accResult);
        System.out.println("끝");
    }


    private List<MabsConditionEntity> makeCondition() {
//        List<String> markets = Arrays.asList("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-EOS", "KRW-ETC", "KRW-ADA", "KRW-MANA", "KRW-BAT", "KRW-BCH", "KRW-DOT");
        List<String> markets = Arrays.asList("KRW-BTC");

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

    @NotNull
    private List<Integer> getConditionSeqList(List<MabsConditionEntity> mabsConditionEntities) {
        return mabsConditionEntities.stream()
                .map(MabsConditionEntity::getConditionSeq)
                .collect(Collectors.toList());
    }

    public List<MabsConditionEntity> backtestSub(List<MabsConditionEntity> mabsConditionEntities, DateRange range) {
        for (MabsConditionEntity condition : mabsConditionEntities) {
            mabsConditionEntityRepository.save(condition);
            List<MabsMultiBacktestRow> tradeHistory = backtestSub(condition, range);

            List<MabsTradeEntity> mabsTradeEntities = convert(condition, tradeHistory);
            mabsTradeEntityRepository.saveAll(mabsTradeEntities);
        }
        return mabsConditionEntities;
    }


    private void checkLastSell(MabsTradeEntity lastTrade) {
        if (lastTrade.getTradeType() == TradeType.BUY) {
            throw new RuntimeException(String.format("마지막 거래가 BUY인 항목이 있음. tradeSeq: %s", lastTrade.getTradeSeq()));
        }
    }

    /**
     * 거래 내역의 마지막이 매수인경우 해당 거래를 삭제
     *
     * @param conditionSeqList 거래 조건 일련번호
     */
    private void deleteLastBuy(List<Integer> conditionSeqList) {
        List<MabsConditionEntity> conditionEntityList = mabsConditionEntityRepository.findAllById(conditionSeqList);

        for (MabsConditionEntity condition : conditionEntityList) {
            List<MabsTradeEntity> tradeList = mabsTradeEntityRepository.findByCondition(condition.getConditionSeq());
            if (tradeList.isEmpty()) {
                continue;
            }
            MabsTradeEntity lastTrade = tradeList.get(tradeList.size() - 1);
            log.info("count: {}, last: {} -> {} ", tradeList.size(), lastTrade.getTradeType(), lastTrade.getTradeTimeKst());
            if (lastTrade.getTradeType() == TradeType.SELL) {
                continue;
            }

            log.info("Delete Last Buy: {} {}", lastTrade.getTradeSeq(), lastTrade.getTradeTimeKst());
            mabsTradeEntityRepository.deleteById(lastTrade.getTradeSeq());
        }
    }

    /**
     * @param condition    거래 조건
     * @param tradeHistory 거래 이력
     * @return 거래 내역 entity 변환
     */
    private List<MabsTradeEntity> convert(MabsConditionEntity condition, List<MabsMultiBacktestRow> tradeHistory) {
        return tradeHistory.stream().map(p -> MabsTradeEntity.builder()
                .conditionEntity(condition)
                .tradeType(p.getTradeEvent())
                .highYield(p.getHighYield())
                .lowYield(p.getLowYield())
                .maShort(p.getMaShort())
                .maLong(p.getMaLong())
                .yield(p.getRealYield())
                .unitPrice(p.getTradeEvent() == TradeType.BUY ? p.getBidPrice() : p.getAskPrice())
                .sellReason(p.getAskReason())
                .tradeTimeKst(p.getCandle().getCandleDateTimeKst())
                .build()).collect(Collectors.toList());
    }

    /**
     * @param condition 조건
     * @param range     백테스트 범위(UTC 기준)
     * @return 거래 내역
     */
    private List<MabsMultiBacktestRow> backtestSub(MabsConditionEntity condition, DateRange range) {
        // key: market, value: 자산
        Map<String, Account> accountMap = new HashMap<>();

        Account cashAccount = new Account();
        cashAccount.setCurrency("KRW");
        cashAccount.setBalance(ApplicationUtil.toNumberString(CASH));
        accountMap.put("KRW", cashAccount);

        Account acc = new Account();
        String market = condition.getMarket();
        String[] tokens = market.split("-");
        acc.setUnitCurrency(tokens[0]);
        acc.setCurrency(tokens[1]);
        acc.setBalance("0");
        accountMap.put(market, acc);

        // Key: market, value: 시세 정보
        Map<String, CurrentPrice> priceMap = new HashMap<>();

        injectionFieldValue(condition);
        List<MabsMultiBacktestRow> tradeHistory = new ArrayList<>();

        tradeEvent.setPriceMap(priceMap);
        tradeEvent.setAccountMap(accountMap);
        tradeEvent.setTradeHistory(tradeHistory);

        accountService.setAccountMap(accountMap);

        LocalDateTime current = range.getFrom();
        LocalDateTime to = range.getTo();
        CandleDataProvider candleDataProvider = new CandleDataProvider(candleRepositoryCustom);

        candleService.setCandleDataProvider(candleDataProvider);

        while (current.isBefore(to) || current.equals(to)) {
            candleDataProvider.setCurrentTime(current);
            CandleMinute candle = candleDataProvider.getCurrentCandle(condition.getMarket());
            if (candle == null) {
                current = current.plusMinutes(1);
                continue;
            }

            TradeResult tradeResult = TradeResult.builder()
                    .type("trade")
                    .code(candle.getMarket())
                    .tradePrice(candle.getTradePrice())
                    .tradeDate(candle.getCandleDateTimeUtc().toLocalDate())
                    .tradeTime(candle.getCandleDateTimeUtc().toLocalTime())
                    // 백테스트에서는 의미없는값
                    .timestamp(0L)
                    .prevClosingPrice(0)
                    .tradeVolume(0)
                    .build();

            mabsMultiService.tradeEvent(tradeResult);
            current = current.plusMinutes(1);
        }
        return tradeHistory;
    }

    private void injectionFieldValue(MabsConditionEntity condition) {
        ReflectionTestUtils.setField(tradeCommonService, "coinByCandles", new HashMap<>());
        ReflectionTestUtils.setField(tradeCommonService, "assetHistoryRepository", this.assetHistoryRepository);
        ReflectionTestUtils.setField(tradeCommonService, "tradeRepository", this.tradeRepository);
        ReflectionTestUtils.setField(mabsMultiService, "tradeCommonService", this.tradeCommonService);
        ReflectionTestUtils.setField(mabsMultiService, "periodIdx", -1);

        MabsMultiProperties properties = mabsMultiService.getProperties();
        properties.setMarkets(Collections.singletonList(condition.getMarket()));
        properties.setMaxBuyCount(1);
        properties.setInvestRatio(0.99);
        properties.setUpBuyRate(condition.getUpBuyRate());
        properties.setLoseStopRate(condition.getLoseStopRate());
        properties.setDownSellRate(condition.getDownSellRate());
        properties.setPeriodType(condition.getTradePeriod());
        properties.setShortPeriod(condition.getShortPeriod());
        properties.setLongPeriod(condition.getLongPeriod());
        properties.setNewMasBuy(true);

        ReflectionTestUtils.setField(mabsMultiService, "properties", properties);

    }

    @RequiredArgsConstructor
    @Getter
    public static class CurrentPrice {
        final Candle candle;
        final double maShort;
        final double maLong;
    }


}
