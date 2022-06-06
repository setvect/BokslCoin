package com.setvect.bokslcoin.autotrading.backtest.neovbs;

import com.setvect.bokslcoin.autotrading.algorithm.BasicTradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.common.TradeCommonService;
import com.setvect.bokslcoin.autotrading.algorithm.neovbs.NeoVbsMultiProperties;
import com.setvect.bokslcoin.autotrading.algorithm.neovbs.NeoVbsMultiService;
import com.setvect.bokslcoin.autotrading.algorithm.websocket.TradeResult;
import com.setvect.bokslcoin.autotrading.backtest.entity.NeoVbsConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.NeoVbsTradeEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.CandleDataProvider;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepository;
import com.setvect.bokslcoin.autotrading.backtest.repository.NeoVbsConditionEntityRepository;
import com.setvect.bokslcoin.autotrading.backtest.repository.NeoVbsTradeEntityRepository;
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
public class NeoVbsBacktest {
    /**
     * 투자금
     */
    public static final double CASH = 10_000_000;

    @Autowired
    private NeoVbsConditionEntityRepository neoVbsConditionEntityRepository;

    @Autowired
    private NeoVbsTradeEntityRepository neoVbsTradeEntityRepository;

    @Autowired
    private CandleRepository candleRepository;

    @Autowired
    private AssetHistoryRepository assetHistoryRepository;

    @Autowired
    private TradeRepository tradeRepository;

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
    private NeoVbsMultiService neoVbsMultiService;

    private List<NeoVbsMultiBacktestRow> tradeHistory;

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

    @Test
    public void 변동성돌파전략_백테스트_DB_저장함() {
        List<String> markets = Arrays.asList("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-EOS", "KRW-ETC", "KRW-ADA", "KRW-MANA", "KRW-BAT", "KRW-BCH", "KRW-DOT");

        for (String market : markets) {
            LocalDateTime baseStart = DateUtil.getLocalDateTime("2022-01-10T00:00:00");
            DateRange range = new DateRange(baseStart, LocalDateTime.now());
            NeoVbsConditionEntity condition = NeoVbsConditionEntity.builder()
                    .market(market)
                    .tradePeriod(PeriodType.PERIOD_1440)
                    .k(0.5)
                    .loseStopRate(0.5)
                    .comment(null)
                    .build();
            neoVbsConditionEntityRepository.save(condition);
            List<NeoVbsMultiBacktestRow> tradeHistory = backtest(condition, range);

            List<NeoVbsTradeEntity> mabsTradeEntities = convert(condition, tradeHistory);
            neoVbsTradeEntityRepository.saveAll(mabsTradeEntities);
        }
        System.out.println("끝");
    }

    /**
     * @param condition    거래 조건
     * @param tradeHistory 거래 이력
     * @return 거래 내역 entity 변환
     */
    private List<NeoVbsTradeEntity> convert(NeoVbsConditionEntity condition, List<NeoVbsMultiBacktestRow> tradeHistory) {
        return tradeHistory.stream().map(p -> NeoVbsTradeEntity.builder()
                .vbsConditionEntity(condition)
                .tradeType(p.getTradeEvent())
                .highYield(p.getHighYield())
                .lowYield(p.getLowYield())
                .targetPrice(p.getTargetPrice())
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
    private List<NeoVbsMultiBacktestRow> backtest(NeoVbsConditionEntity condition, DateRange range) {
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

            neoVbsMultiService.tradeEvent(tradeResult);
            current = current.plusMinutes(1);
            count++;
        }

        Mockito.reset(candleService, orderService, accountService, tradeEvent);
        return tradeHistory;
    }

    private void injectionFieldValue(NeoVbsConditionEntity condition) {
        ReflectionTestUtils.setField(tradeCommonService, "coinByCandles", new HashMap<>());
        ReflectionTestUtils.setField(tradeCommonService, "assetHistoryRepository", this.assetHistoryRepository);
        ReflectionTestUtils.setField(tradeCommonService, "tradeRepository", this.tradeRepository);
        ReflectionTestUtils.setField(neoVbsMultiService, "tradeCommonService", this.tradeCommonService);
        ReflectionTestUtils.setField(neoVbsMultiService, "periodIdx", -1);

        NeoVbsMultiProperties properties = new NeoVbsMultiProperties();
        properties.setMarkets(Collections.singletonList(condition.getMarket()));
        properties.setMaxBuyCount(1);
        properties.setInvestRatio(0.99);
        properties.setLoseStopRate(condition.getLoseStopRate());
        properties.setPeriodType(condition.getTradePeriod());
        properties.setK(condition.getK());
        ReflectionTestUtils.setField(neoVbsMultiService, "properties", properties);

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
            priceMap.put(currentCandle.getMarket(), new CurrentPrice(currentCandle));
            return null;
            // TODO check 사용할수 있게 이벤트 적용
        }).when(tradeEvent).check(notNull());


        // 매수
        doAnswer(invocation -> {
            String market = invocation.getArgument(0, String.class);
            CurrentPrice currentPrice = priceMap.get(market);
            Candle candle = currentPrice.getCandle();
            NeoVbsMultiBacktestRow backtestRow = new NeoVbsMultiBacktestRow(depthCopy(candle));
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

            NeoVbsMultiBacktestRow backtestRow = new NeoVbsMultiBacktestRow(depthCopy(candle));

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
    }
}
