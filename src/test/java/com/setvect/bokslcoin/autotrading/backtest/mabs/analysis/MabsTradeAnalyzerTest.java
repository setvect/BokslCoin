package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis;

import com.setvect.bokslcoin.autotrading.algorithm.BasicTradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.TradePeriod;
import com.setvect.bokslcoin.autotrading.algorithm.mabs.MabsMultiService;
import com.setvect.bokslcoin.autotrading.backtest.entity.MabsConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.MabsTradeEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.mabs.multi.CandleDataProvider;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepository;
import com.setvect.bokslcoin.autotrading.backtest.repository.MabsConditionEntityRepository;
import com.setvect.bokslcoin.autotrading.backtest.repository.MabsTradeEntityRepository;
import com.setvect.bokslcoin.autotrading.exchange.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.OrderService;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import com.setvect.bokslcoin.autotrading.record.repository.AssetHistoryRepository;
import com.setvect.bokslcoin.autotrading.record.repository.TradeRepository;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import com.setvect.bokslcoin.autotrading.util.MathUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("local")
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

    @Mock
    private SlackMessageService slackMessageService;

    @Mock
    private AccountService accountService;

    @Mock
    private CandleService candleService;

    @Mock
    private OrderService orderService;

    @Spy
    private final TradeEvent tradeEvent = new BasicTradeEvent(slackMessageService);

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

    @Test
    public void backtest() {
        List<String> coinList = Arrays.asList("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-EOS", "KRW-ETC");

        List<Pair<Integer, Integer>> periodList = new ArrayList<>();
        periodList.add(new ImmutablePair<>(20, 80));
        periodList.add(new ImmutablePair<>(22, 80));
        periodList.add(new ImmutablePair<>(24, 80));
        periodList.add(new ImmutablePair<>(20, 90));
        periodList.add(new ImmutablePair<>(22, 90));
        periodList.add(new ImmutablePair<>(24, 90));

        for (Pair<Integer, Integer> period : periodList) {
            for (String coin : coinList) {
                MabsConditionEntity condition = MabsConditionEntity.builder()
                        .market(coin)
                        .analysisFrom(DateUtil.getLocalDateTime("2017-10-01T00:00:00"))
                        .analysisTo(DateUtil.getLocalDateTime("2021-12-18T23:59:59"))
                        .tradePeriod(TradePeriod.P_30)
                        .upBuyRate(0.01)
                        .downSellRate(0.01)
                        .shortPeriod(period.getLeft())
                        .longPeriod(period.getRight())
                        .loseStopRate(0.3)
                        .comment(null)
                        .build();
                mabsConditionEntityRepository.save(condition);
                List<MabsMultiBacktestRow> tradeHistory = backtest(condition);

                List<MabsTradeEntity> mabsTradeEntities = convert(condition, tradeHistory);
                mabsTradeEntityRepository.saveAll(mabsTradeEntities);
            }
        }
        System.out.println("끝");
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
                .candleDateTimeKst(p.getCandle().getCandleDateTimeKst())
                .build()).collect(Collectors.toList());
    }


    private List<MabsMultiBacktestRow> backtest(MabsConditionEntity condition) {
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

        LocalDateTime current = condition.getAnalysisFrom();
        LocalDateTime to = condition.getAnalysisTo();
        CandleDataProvider candleDataProvider = new CandleDataProvider(candleRepository);

        initMock(candleDataProvider);

        int count = 0;
        while (current.isBefore(to) || current.equals(to)) {
            if (count == 1440 * 50) {
                log.info("clear...");
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

            mabsMultiService.apply();
            current = current.plusMinutes(1);
            count++;
        }

        Mockito.reset(candleService, orderService, accountService, tradeEvent);
        return tradeHistory;
    }


    /**
     * @param amounts 실제 코인 변화 가격
     * @return 코인별 수익률, MDD
     */
    private static TestAnalysisMulti.YieldMdd calculateCoinYieldMdd(List<Double> amounts) {
        if (amounts.isEmpty()) {
            return new TestAnalysisMulti.YieldMdd();
        }
        TestAnalysisMulti.YieldMdd yield = new TestAnalysisMulti.TotalYield();
        yield.setYield(MathUtil.getYield(amounts.get(amounts.size() - 1), amounts.get(0)));
        yield.setMdd(ApplicationUtil.getMdd(amounts));
        return yield;
    }

    /**
     * @param tradeHistory 매매 기록
     * @param condition    투자 조건
     * @return 대상코인의 수익률 정보를 제공
     */
    private static TestAnalysisMulti.TotalYield calculateTotalInvestment(List<MabsMultiBacktestRow> tradeHistory, MabsConditionEntity condition) {
        List<Double> amountHistory = new ArrayList<>();
        amountHistory.add(tradeHistory.get(0).getFinalResult());
        amountHistory.addAll(tradeHistory.stream().skip(1).map(MabsMultiBacktestRow::getFinalResult).collect(Collectors.toList()));
        TestAnalysisMulti.TotalYield totalYield = new TestAnalysisMulti.TotalYield();
        double realYield = tradeHistory.get(tradeHistory.size() - 1).getFinalResult() / tradeHistory.get(0).getFinalResult() - 1;
        double realMdd = ApplicationUtil.getMdd(amountHistory);
        totalYield.setYield(realYield);
        totalYield.setMdd(realMdd);

        // 승률
        for (MabsMultiBacktestRow row : tradeHistory) {
            if (row.getTradeEvent() != TradeType.SELL) {
                continue;
            }
            if (row.getRealYield() > 0) {
                totalYield.setGainCount(totalYield.getGainCount() + 1);
            } else {
                totalYield.setLossCount(totalYield.getLossCount() + 1);
            }
        }

        long dayCount = ChronoUnit.DAYS.between(condition.getAnalysisFrom(), condition.getAnalysisTo());
        totalYield.setDayCount((int) dayCount);
        return totalYield;
    }

    private void injectionFieldValue(MabsConditionEntity condition) {
        ReflectionTestUtils.setField(mabsMultiService, "assetHistoryRepository", this.assetHistoryRepository);
        ReflectionTestUtils.setField(mabsMultiService, "tradeRepository", this.tradeRepository);
        ReflectionTestUtils.setField(mabsMultiService, "markets", Collections.singletonList(condition.getMarket()));
        ReflectionTestUtils.setField(mabsMultiService, "maxBuyCount", 1);
        ReflectionTestUtils.setField(mabsMultiService, "investRatio", 0.99);
        ReflectionTestUtils.setField(mabsMultiService, "upBuyRate", condition.getUpBuyRate());
        ReflectionTestUtils.setField(mabsMultiService, "loseStopRate", condition.getLoseStopRate());
        ReflectionTestUtils.setField(mabsMultiService, "downSellRate", condition.getDownSellRate());
        ReflectionTestUtils.setField(mabsMultiService, "tradePeriod", condition.getTradePeriod());
        ReflectionTestUtils.setField(mabsMultiService, "shortPeriod", condition.getShortPeriod());
        ReflectionTestUtils.setField(mabsMultiService, "longPeriod", condition.getLongPeriod());
        ReflectionTestUtils.setField(mabsMultiService, "periodIdx", -1);
        ReflectionTestUtils.setField(mabsMultiService, "slackTime", "08:00");
        ReflectionTestUtils.setField(mabsMultiService, "newMasBuy", true);
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
        }).when(tradeEvent).check(notNull(), anyDouble(), anyDouble());


        // 매수
        doAnswer(invocation -> {
            String market = invocation.getArgument(0, String.class);
            CurrentPrice currentPrice = priceMap.get(market);
            Candle candle = currentPrice.getCandle();
            MabsMultiBacktestRow backtestRow = new MabsMultiBacktestRow(candle);

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

            MabsMultiBacktestRow backtestRow = new MabsMultiBacktestRow(candle);

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
