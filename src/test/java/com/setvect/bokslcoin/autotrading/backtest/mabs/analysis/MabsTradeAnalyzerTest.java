package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis;

import com.setvect.bokslcoin.autotrading.algorithm.BasicTradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.common.TradeCommonService;
import com.setvect.bokslcoin.autotrading.algorithm.mabs.MabsMultiProperties;
import com.setvect.bokslcoin.autotrading.algorithm.mabs.MabsMultiService;
import com.setvect.bokslcoin.autotrading.algorithm.websocket.TradeResult;
import com.setvect.bokslcoin.autotrading.backtest.common.BacktestHelperComponent;
import com.setvect.bokslcoin.autotrading.backtest.entity.MabsConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.MabsTradeEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepository;
import com.setvect.bokslcoin.autotrading.backtest.repository.MabsConditionEntityRepository;
import com.setvect.bokslcoin.autotrading.backtest.repository.MabsTradeEntityQuerydslRepository;
import com.setvect.bokslcoin.autotrading.backtest.repository.MabsTradeEntityRepository;
import com.setvect.bokslcoin.autotrading.exchange.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.OrderService;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import com.setvect.bokslcoin.autotrading.record.repository.AssetHistoryRepository;
import com.setvect.bokslcoin.autotrading.record.repository.TradeRepository;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class MabsTradeAnalyzerTest {
    /**
     * 투자금
     */
    public static final double CASH = 10_000_000;

    @Autowired
    private MabsConditionEntityRepository mabsConditionEntityRepository;

    @Autowired
    private MabsTradeEntityRepository mabsTradeEntityRepository;

    @Autowired
    private CandleRepository candleRepository;

    @Autowired
    private AssetHistoryRepository assetHistoryRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private BacktestHelperComponent backtestHelperService;

    @Autowired
    private MakeBacktestReportService makeBacktestReportService;

    @Autowired
    private MabsTradeEntityQuerydslRepository mabsTradeEntityQuerydslRepository;


    @Mock
    private SlackMessageService slackMessageService;
    @Spy
    private final TradeEvent tradeEvent = new BasicTradeEvent(slackMessageService);
    @Mock
    private AccountService accountService;
    @Mock
    private CandleService candleService;
    @Mock
    private OrderService orderService;
    @InjectMocks
    private TradeCommonService tradeCommonService;

    @InjectMocks
    private MabsMultiService mabsMultiService;

    private List<MabsMultiBacktestRow> tradeHistory;

    /**
     * 최고 수익률
     */
    private double highYield;
    /**
     * 최저 수익률
     */
    private double lowYield;
    private Map<String, CurrentPrice> priceMap;
    private Map<String, Account> accountMap;

    private static Candle depthCopy(Candle candle) {
        String json = GsonUtil.GSON.toJson(candle);
        return GsonUtil.GSON.fromJson(json, Candle.class);
    }

    @DisplayName("백테스트 결과를 DB에 저장하지 않음")
    @Test
    public void backtestNoSave() {
        List<MabsConditionEntity> mabsConditionEntities = null;
        try {
            mabsConditionEntities = backtest();
            List<Integer> conditionSeqList = getConditionSeqList(mabsConditionEntities);

            AnalysisMultiCondition analysisMultiCondition = AnalysisMultiCondition.builder()
                    .conditionIdSet(new HashSet<>(conditionSeqList))
                    .range(new DateRange(DateUtil.getLocalDateTime("2022-01-10T00:00:00"), DateUtil.getLocalDateTime("2022-06-02T23:59:59")))
                    .investRatio(.99)
                    .cash(14_223_714)
                    .feeSell(0.002) // 슬립피지까지 고려해 보수적으로 0.2% 수수료 측정
                    .feeBuy(0.002)
                    .build();

            makeBacktestReportService.makeReport(analysisMultiCondition);
        } finally {
            if (CollectionUtils.isEmpty(mabsConditionEntities)) {
                return;
            }
            List<Integer> conditionSeqList = getConditionSeqList(mabsConditionEntities);
            // 결과를 삭제함
            // TODO 너무 무식한 방법이다. @Transactional를 사용해야 되는데 사용하면 속도가 매우 느리다. 해결해야됨
            mabsTradeEntityQuerydslRepository.deleteByConditionId(conditionSeqList);
            mabsConditionEntityRepository.deleteAll(mabsConditionEntities);
        }
    }

    @NotNull
    private List<Integer> getConditionSeqList(List<MabsConditionEntity> mabsConditionEntities) {
        return mabsConditionEntities.stream()
                .map(MabsConditionEntity::getMabsConditionSeq)
                .collect(Collectors.toList());
    }

    @DisplayName("백테스트 결과를 DB에 저장함")
    @Test
    public void backtestSave() {
        backtest();
    }


    public List<MabsConditionEntity> backtest() {
//        List<String> markets = Arrays.asList("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-EOS", "KRW-ETC", "KRW-ADA", "KRW-MANA", "KRW-BAT", "KRW-BCH", "KRW-DOT");
        List<String> markets = Arrays.asList("KRW-BTC");

        List<Pair<Integer, Integer>> periodList = new ArrayList<>();
        periodList.add(new ImmutablePair<>(13, 64));

        List<MabsConditionEntity> mabsConditionEntities = new ArrayList<>();
        for (Pair<Integer, Integer> period : periodList) {
            for (String market : markets) {
                log.info("{} - {} start", period, market);
//                LocalDateTime baseStart = backtestHelperService.makeBaseStart(market, PeriodType.PERIOD_60, period.getRight() + 1);
//                LocalDateTime baseStart = DateUtil.getLocalDateTime("2022-01-10T00:00:00");
//                LocalDateTime baseEnd = DateUtil.getLocalDateTime("2022-06-02T23:59:59");
                LocalDateTime baseStart = DateUtil.getLocalDateTime("2022-03-01T00:00:00");
                LocalDateTime baseEnd = DateUtil.getLocalDateTime("2022-06-01T00:00:00");

                DateRange range = new DateRange(baseStart, baseEnd);
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
                mabsConditionEntityRepository.save(condition);
                mabsConditionEntities.add(condition);
                List<MabsMultiBacktestRow> tradeHistory = backtest(condition, range);

                List<MabsTradeEntity> mabsTradeEntities = convert(condition, tradeHistory);
                mabsTradeEntityRepository.saveAll(mabsTradeEntities);
            }
        }
        return mabsConditionEntities;
    }

    @Test
    public void 특정_조건에_대해_증분_분석_수행() {
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
            List<MabsTradeEntity> tradeList = mabsTradeEntityRepository.findByCondition(condition.getMabsConditionSeq());

            LocalDateTime start = backtestHelperService.makeBaseStart(condition.getMarket(), condition.getTradePeriod(), condition.getLongPeriod() + 1);
            if (!tradeList.isEmpty()) {
                MabsTradeEntity lastTrade = tradeList.get(tradeList.size() - 1);
                checkLastSell(lastTrade);
                start = lastTrade.getTradeTimeKst();
            }
            DateRange range = new DateRange(start, LocalDateTime.now());
            List<MabsMultiBacktestRow> tradeHistory = backtest(condition, range);

            List<MabsTradeEntity> mabsTradeEntities = convert(condition, tradeHistory);
            log.info("[{}] save. range: {}, trade Count: {}", condition.getMarket(), range, mabsTradeEntities.size());
            mabsTradeEntityRepository.saveAll(mabsTradeEntities);
        }
        log.info("끝.");
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
            List<MabsTradeEntity> tradeList = mabsTradeEntityRepository.findByCondition(condition.getMabsConditionSeq());
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
                .mabsConditionEntity(condition)
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
    private List<MabsMultiBacktestRow> backtest(MabsConditionEntity condition, DateRange range) {
        // key: market, value: 자산
        accountMap = new HashMap<>();

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
        priceMap = new HashMap<>();

        injectionFieldValue(condition);
        tradeHistory = new ArrayList<>();

        LocalDateTime current = range.getFrom();
        LocalDateTime to = range.getTo();
        CandleDataProvider candleDataProvider = new CandleDataProvider(candleRepository);

        initMock(candleDataProvider);

        int count = 0;
        while (current.isBefore(to) || current.equals(to)) {
            if (count == 1440 * 7) {
                log.info("clear: {}, {}, {}", condition.getMarket(), current, count);
                Mockito.reset(candleService, orderService, accountService, tradeEvent);
                initMock(candleDataProvider);
                count = 0;
            }

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
            count++;
        }

        Mockito.reset(candleService, orderService, accountService, tradeEvent);
        return tradeHistory;
    }

    private void injectionFieldValue(MabsConditionEntity condition) {
        ReflectionTestUtils.setField(tradeCommonService, "coinByCandles", new HashMap<>());
        ReflectionTestUtils.setField(tradeCommonService, "assetHistoryRepository", this.assetHistoryRepository);
        ReflectionTestUtils.setField(tradeCommonService, "tradeRepository", this.tradeRepository);
        ReflectionTestUtils.setField(mabsMultiService, "tradeCommonService", this.tradeCommonService);
        ReflectionTestUtils.setField(mabsMultiService, "periodIdx", -1);

        MabsMultiProperties properties = new MabsMultiProperties();
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

    private void initMock(CandleDataProvider candleDataProvider) {
        when(candleService.getMinute(anyInt(), anyString()))
                .then((invocation) -> candleDataProvider.getCurrentCandle(invocation.getArgument(1)));

        when(candleService.getMinute(eq(15), anyString(), anyInt()))
                .then((invocation) -> candleDataProvider.beforeMinute(invocation.getArgument(1, String.class), PeriodType.PERIOD_15, invocation.getArgument(2, Integer.class)));
        when(candleService.getMinute(eq(30), anyString(), anyInt()))
                .then((invocation) -> candleDataProvider.beforeMinute(invocation.getArgument(1, String.class), PeriodType.PERIOD_30, invocation.getArgument(2, Integer.class)));
        when(candleService.getMinute(eq(60), anyString(), anyInt()))
                .then((invocation) -> candleDataProvider.beforeMinute(invocation.getArgument(1, String.class), PeriodType.PERIOD_60, invocation.getArgument(2, Integer.class)));
        when(candleService.getMinute(eq(240), anyString(), anyInt()))
                .then((invocation) -> candleDataProvider.beforeMinute(invocation.getArgument(1, String.class), PeriodType.PERIOD_240, invocation.getArgument(2, Integer.class)));

        when(candleService.getDay(anyString()))
                .then((invocation) -> {
                    List<CandleDay> candleDays = candleDataProvider.beforeDayCandle(invocation.getArgument(0, String.class), 2);
                    return candleDays.get(0);
                });
        when(candleService.getDay(anyString(), anyInt()))
                .then((invocation) -> candleDataProvider.beforeDayCandle(invocation.getArgument(0, String.class), invocation.getArgument(1, Integer.class)));

        // 현재 가지고있는 자산 조회
        when(accountService.getMyAccountBalance()).then((method) -> accountMap.entrySet().stream()
                .filter(e -> e.getValue().getBalanceValue() != 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));


        // 시세 체크
        doAnswer(invocation -> {
            Candle currentCandle = invocation.getArgument(0);
            double maShort = invocation.getArgument(1, Double.class);
            double maLong = invocation.getArgument(2, Double.class);
            priceMap.put(currentCandle.getMarket(), new CurrentPrice(currentCandle, maShort, maLong));
            return null;
            // TODO check 사용할수 있게 이벤트 적용
        }).when(tradeEvent).check(notNull(), anyDouble(), anyDouble());


        // 매수
        doAnswer(invocation -> {
            String market = invocation.getArgument(0, String.class);
            CurrentPrice currentPrice = priceMap.get(market);
            Candle candle = currentPrice.getCandle();
            MabsMultiBacktestRow backtestRow = new MabsMultiBacktestRow(depthCopy(candle));
            double tradePrice = invocation.getArgument(1);

            Account coinAccount = accountMap.get(market);
            coinAccount.setAvgBuyPrice(ApplicationUtil.toNumberString(tradePrice));
            double investAmount = invocation.getArgument(2);

            Account krwAccount = accountMap.get("KRW");
            double cash = Double.parseDouble(krwAccount.getBalance()) - investAmount;
            krwAccount.setBalance(ApplicationUtil.toNumberString(cash));

            String balance = ApplicationUtil.toNumberString(investAmount / tradePrice);
            coinAccount.setBalance(balance);

            backtestRow.setTradeEvent(TradeType.BUY);
            backtestRow.setBidPrice(tradePrice);
            backtestRow.setBuyAmount(investAmount);
            backtestRow.setBuyTotalAmount(getBuyTotalAmount(accountMap));
            backtestRow.setCash(cash);
            backtestRow.setMaShort(currentPrice.getMaShort());
            backtestRow.setMaLong(currentPrice.getMaLong());

            tradeHistory.add(backtestRow);

            return null;
        }).when(tradeEvent).bid(anyString(), anyDouble(), anyDouble());

        // 최고수익률
        doAnswer(invocation -> {
            this.highYield = invocation.getArgument(1, Double.class);
            return null;
        }).when(tradeEvent).highYield(anyString(), anyDouble());

        // 최저 수익률
        doAnswer(this::answer).when(tradeEvent).lowYield(anyString(), anyDouble());

        // 매도
        doAnswer(invocation -> {
            String market = invocation.getArgument(0, String.class);
            CurrentPrice currentPrice = priceMap.get(market);
            Candle candle = currentPrice.getCandle();

            MabsMultiBacktestRow backtestRow = new MabsMultiBacktestRow(depthCopy(candle));

            Account coinAccount = accountMap.get(market);
            backtestRow.setBidPrice(coinAccount.getAvgBuyPriceValue());
            backtestRow.setBuyAmount(coinAccount.getInvestCash());

            double tradePrice = invocation.getArgument(2);
            double balance = Double.parseDouble(coinAccount.getBalance());
            double askAmount = tradePrice * balance;

            Account krwAccount = accountMap.get("KRW");
            double totalCash = Double.parseDouble(krwAccount.getBalance()) + askAmount;
            krwAccount.setBalance(ApplicationUtil.toNumberString(totalCash));
            coinAccount.setBalance("0");
            coinAccount.setAvgBuyPrice(null);

            backtestRow.setBuyTotalAmount(getBuyTotalAmount(accountMap));
            backtestRow.setTradeEvent(TradeType.SELL);
            backtestRow.setAskPrice(tradePrice);
            backtestRow.setCash(krwAccount.getBalanceValue());
            backtestRow.setAskReason(invocation.getArgument(3));
            backtestRow.setMaShort(currentPrice.getMaShort());
            backtestRow.setMaLong(currentPrice.getMaLong());
            backtestRow.setHighYield(highYield);
            backtestRow.setLowYield(lowYield);

            tradeHistory.add(backtestRow);
            return null;
        }).when(tradeEvent).ask(anyString(), anyDouble(), anyDouble(), notNull());
    }

    /**
     * @param accountMap 코인(현금 포함) 계좌
     * @return 현재 투자한 코인 함
     */
    private double getBuyTotalAmount(Map<String, Account> accountMap) {
        return accountMap.entrySet().stream().filter(e -> !e.getKey().equals("KRW")).mapToDouble(e -> e.getValue().getInvestCash()).sum();
    }

    private Object answer(InvocationOnMock invocation) {
        this.lowYield = invocation.getArgument(1, Double.class);
        return null;
    }

    @RequiredArgsConstructor
    @Getter
    static class CurrentPrice {
        final Candle candle;
        final double maShort;
        final double maLong;
    }


}
