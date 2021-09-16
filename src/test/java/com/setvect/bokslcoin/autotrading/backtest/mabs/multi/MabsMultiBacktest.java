package com.setvect.bokslcoin.autotrading.backtest.mabs.multi;

import com.setvect.bokslcoin.autotrading.algorithm.BasicTradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.TradePeriod;
import com.setvect.bokslcoin.autotrading.algorithm.mabs.MabsMultiService;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepository;
import com.setvect.bokslcoin.autotrading.exchange.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.OrderService;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import com.setvect.bokslcoin.autotrading.record.repository.AssetHistoryRepository;
import com.setvect.bokslcoin.autotrading.record.repository.TradeRepository;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import com.setvect.bokslcoin.autotrading.util.MathUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class MabsMultiBacktest {

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
     * 코인별 가격
     * Key: market, value: 시세 정보
     */
    private Map<String, List<Double>> amountByCoin;
    private Map<String, Double> highYieldMap;
    private Map<String, Double> lowYieldMap;
    private Map<String, CurrentPrice> priceMap;
    private Map<String, Account> accountMap;

    @Test
    public void singleBacktest() throws IOException {
        // === 1. 변수값 설정 ===
        MabsMultiCondition condition = MabsMultiCondition.builder()
                .range(new DateRange("2021-06-07T00:00:00", "2021-06-30T23:59:59"))
//                .range(new DateRange("2020-11-01T00:00:00", "2021-0>>7-14T23:59:59"))
//                .range(new DateRange("2021-06-14T00:00:00", "2021-07-07T23:59:59"))
//                .range(new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59")) // 상승후 하락
//                .range(new DateRange("2020-11-01T00:00:00", "2021-04-14T23:59:59")) // 상승장
//                .range(new DateRange("2020-05-07T00:00:00", "2020-10-20T23:59:59")) // 횡보장1
//                .range(new DateRange("2020-05-08T00:00:00", "2020-07-26T23:59:59")) // 횡보장2
//                .range(new DateRange("2018-01-01T00:00:00", "2020-11-19T23:59:59")) // 횡보장3
//                .range(new DateRange("2019-06-24T00:00:00", "2020-03-31T23:59:59")) // 횡보+하락장1
//                .range(new DateRange("2017-12-24T00:00:00", "2020-03-31T23:59:59")) // 횡보+하락장2
//                .range(new DateRange("2021-04-14T00:00:00", "2021-06-08T23:59:59")) // 하락장1
//                .range(new DateRange("2017-12-07T00:00:00", "2018-02-06T23:59:59")) // 하락장2
//                .range(new DateRange("2018-01-06T00:00:00", "2018-02-06T23:59:59")) // 하락장3
//                .range(new DateRange("2018-01-06T00:00:00", "2018-12-15T23:59:59")) // 하락장4(찐하락장)
//                .range(new DateRange("2019-06-27T00:00:00", "2020-03-17T23:59:59")) // 하락장5
//                .range(new DateRange("2018-01-06T00:00:00", "2019-08-15T23:59:59")) // 하락장 이후 약간의 상승장
//                .range(new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59")) // 전체 기간

                .markets(Arrays.asList("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-EOS", "KRW-ETC"))// 대상 코인
                .investRatio(0.99) // 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
                .maxBuyCount(5)
                .cash(10_000_000) // 최초 투자 금액
                .tradeMargin(0)// 매매시 채결 가격 차이
                // 슬리피지를 고려해 수수료 올림
                .feeBid(0.0007) //  매수 수수료
                .feeAsk(0.0007)//  매도 수수료
                .loseStopRate(0.99) // 손절 라인
                .upBuyRate(0.01) //상승 매수율
                .downSellRate(0.01) // 하락 매도률
                .shortPeriod(13) // 단기 이동평균 기간
                .longPeriod(64) // 장기 이동평균 기간
                .tradePeriod(TradePeriod.P_60) //매매 주기
                .build();

        // === 2. 백테스트 ===
        TestAnalysisMulti testAnalysis = backtest(condition);
        System.out.println(condition.toString());

        for (String market : condition.getMarkets()) {
            TestAnalysisMulti.YieldMdd coinYield = testAnalysis.getCoinYield(market);
            System.out.printf("[%s] 실제 수익: %,.2f%%\n", market, coinYield.getYield() * 100);
            System.out.printf("[%s] 실제 MDD: %,.2f%%\n", market, coinYield.getMdd() * 100);
        }

        for (String market : condition.getMarkets()) {
            TestAnalysisMulti.CoinInvestment coinInvestment = testAnalysis.getCoinInvestment(market);
            System.out.printf("[%s] 수익금액 합계: %,.0f\n", market, coinInvestment.getInvest());
            System.out.printf("[%s] 매매 횟수: %d\n", market, coinInvestment.getTradeCount());
            System.out.printf("[%s] 승률: %,.2f%%\n", market, coinInvestment.getWinRate() * 100);
        }

        System.out.printf("실현 수익: %,.2f%%\n", testAnalysis.getTotal().getYield() * 100);
        System.out.printf("실현 MDD: %,.2f%%\n", testAnalysis.getTotal().getMdd() * 100);
        System.out.printf("매매 횟수: %d\n", testAnalysis.getTotal().getTradeCount());
        System.out.printf("승률: %,.2f%%\n", testAnalysis.getTotal().getWinRate() * 100);
        System.out.printf("CAGR: %,.2f%%\n", testAnalysis.getTotal().getCagr() * 100);


        // === 3. 리포트 ===
        makeReport(condition, tradeHistory, testAnalysis);

        System.out.println("끝");
    }


    @Test
    public void multiBacktest() throws IOException {
        String header = "분석기간,분석주기,대상 코인,투자비율,최대 코인 매매 갯수,최초 투자금액,매매 마진,매수 수수료,매도 수수료,상승 매수률,하락 매도률,손절,단기 이동평균 기간,장기 이동평균 기간,조건 설명,실현 수익,실현 MDD,매매 횟수,승률,CAGR";

        StringBuffer report = new StringBuffer(header.replace(",", "\t") + "\n");
        MabsMultiCondition condition;

        List<DateRange> rangeList = Arrays.asList(
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
                new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59") // 전체 기간
        );
        List<List<String>> marketsList = new ArrayList<>();
        marketsList.add(Arrays.asList("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-EOS", "KRW-ETC"));
//        marketsList.add(Arrays.asList("KRW-XRP", "KRW-EOS"));
//        marketsList.add(Arrays.asList("KRW-ETH", "KRW-ETC"));

        int count = 0;
        Date now = new Date();
        int[] shortPeriod = {13};
        int[] longPeriod = {64};
        double[] rateList = {0.008, 0.009, 0.011};
        for (int sp : shortPeriod) {
            for (int lp : longPeriod) {
                for (double rate : rateList) {
                    for (List<String> markets : marketsList) {
                        for (DateRange range : rangeList) {
                            condition = MabsMultiCondition.builder()
                                    .markets(markets)// 대상 코인
                                    .range(range)
                                    .maxBuyCount(markets.size())
                                    .investRatio(0.99) // 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
                                    .cash(10_000_000) // 최초 투자 금액
                                    .tradeMargin(0)// 매매시 채결 가격 차이
                                    // 슬리피지를 고려해 수수료 올림
                                    .feeBid(0.0007) //  매수 수수료
                                    .feeAsk(0.0007)//  매도 수수료
                                    .loseStopRate(0.99) // 손절 라인
                                    .upBuyRate(rate) //상승 매수율
                                    .downSellRate(rate) // 하락 매도률
                                    .shortPeriod(sp) // 단기 이동평균 기간
                                    .longPeriod(lp) // 장기 이동평균 기간
                                    .tradePeriod(TradePeriod.P_60) //매매 주기
                                    .build();
                            log.info(condition.toString());

                            TestAnalysisMulti testAnalysis = backtest(condition);
                            report.append(getReportRow(condition, testAnalysis) + "\n");
                            makeReport(condition, tradeHistory, testAnalysis);

                            // -- 결과 저장 --
                            File reportFile = new File("./backtest-result", "이평선돌파_전략_백테스트_분석결과_" + (++count) + "_" + now.getTime() + ".txt");
                            FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr");
                            System.out.println("결과 파일:" + reportFile.getName());
                        }
                        report.append("\n");
                    }
                }
            }
        }
        // -- 결과 저장 --
        File reportFile = new File("./backtest-result", "이평선돌파_전략_백테스트_분석결과_" + now.getTime() + ".txt");
        FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr");
        System.out.println("결과 파일:" + reportFile.getName());

        System.out.println("끝");
    }

    private TestAnalysisMulti backtest(MabsMultiCondition condition) {
        // key: market, value: 자산
        accountMap = new HashMap<>();

        Account cashAccount = new Account();
        cashAccount.setCurrency("KRW");
        cashAccount.setBalance(ApplicationUtil.toNumberString(condition.getCash()));
        accountMap.put("KRW", cashAccount);

        for (String market : condition.getMarkets()) {
            Account acc = new Account();
            String[] tokens = market.split("-");
            acc.setUnitCurrency(tokens[0]);
            acc.setCurrency(tokens[1]);
            acc.setBalance("0");
            accountMap.put(market, acc);
        }

        // Key: market, value: 시세 정보
        priceMap = new HashMap<>();

        // 코인별 가격
        amountByCoin = condition.getMarkets().stream().collect(Collectors.toMap(p -> p, p -> new ArrayList<>()));

        // key: market, value: 최고 수익률
        highYieldMap = new HashMap<>();
        // key: market, value: 최저 수익률
        lowYieldMap = new HashMap<>();


        injectionFieldValue(condition);
        tradeHistory = new ArrayList<>();

        LocalDateTime current = condition.getRange().getFrom();
        LocalDateTime to = condition.getRange().getTo();
        CandleDataProvider candleDataProvider = new CandleDataProvider(candleRepository);

        initMock(condition, candleDataProvider);

        int count = 0;
        while (current.isBefore(to) || current.equals(to)) {
            if (count == 1440 * 50) {
                log.info("clear...");
                Mockito.reset(candleService, orderService, accountService, tradeEvent);
                initMock(condition, candleDataProvider);
                count = 0;
            }

            candleDataProvider.setCurrentTime(current);
            CandleMinute candle = candleDataProvider.getCurrentCandle(condition.getMarkets().get(0));
            if (candle == null) {
                current = current.plusMinutes(1);
                continue;
            }

            mabsMultiService.apply();
            current = current.plusMinutes(1);
            count++;
        }

        Mockito.reset(candleService, orderService, accountService, tradeEvent);
        return analysis(tradeHistory, condition, amountByCoin);
    }

    public static TestAnalysisMulti analysis(List<MabsMultiBacktestRow> tradeHistory, MabsMultiCondition condition, Map<String, List<Double>> amountByCoin) {
        TestAnalysisMulti testAnalysis = new TestAnalysisMulti();
        if (tradeHistory.isEmpty()) {
            return testAnalysis;
        }

        // 대상 코인별 수익률 계산
        for (String market : condition.getMarkets()) {
            TestAnalysisMulti.YieldMdd yield = getCoinYieldMdd(amountByCoin, market);
            testAnalysis.addCoinYieldMdd(market, yield);

            TestAnalysisMulti.CoinInvestment coinInvestment = getCoinInvestment(market, tradeHistory);
            testAnalysis.addCoinInvestment(market, coinInvestment);
        }

        TestAnalysisMulti.TotalYield totalYield = getCoinYield(tradeHistory, condition);
        testAnalysis.setTotal(totalYield);

        return testAnalysis;
    }

    /**
     * @param amountByCoin
     * @param market
     * @return 코인별 수익률, MDD
     */
    private static TestAnalysisMulti.YieldMdd getCoinYieldMdd(Map<String, List<Double>> amountByCoin, String market) {
        List<Double> amounts = amountByCoin.get(market);
        if (amounts.isEmpty()) {
            return new TestAnalysisMulti.YieldMdd();
        }
        TestAnalysisMulti.YieldMdd yield = new TestAnalysisMulti.TotalYield();
        yield.setYield(MathUtil.getYield(amounts.get(amounts.size() - 1), amounts.get(0)));
        yield.setMdd(ApplicationUtil.getMdd(amounts));
        return yield;
    }

    /**
     * @param market
     * @param tradeHistory
     * @return 코인별 수익률
     */
    private static TestAnalysisMulti.CoinInvestment getCoinInvestment(String market, List<MabsMultiBacktestRow> tradeHistory) {
        List<MabsMultiBacktestRow> filter = tradeHistory.stream()
                .filter(p -> p.getCandle().getMarket().equals(market))
                .filter(p -> p.getTradeEvent() == MabsMultiBacktestRow.TradeEvent.SELL)
                .collect(Collectors.toList());
        double totalInvest = filter.stream().mapToDouble(p -> p.getGains()).sum();
        int gainCount = (int) filter.stream().filter(p -> p.getGains() > 0).count();
        TestAnalysisMulti.CoinInvestment coinInvestment = new TestAnalysisMulti.CoinInvestment();
        coinInvestment.setInvest(totalInvest);
        coinInvestment.setGainCount(gainCount);
        coinInvestment.setLossCount(filter.size() - gainCount);
        return coinInvestment;
    }

    /**
     * @param tradeHistory
     * @param condition
     * @return 대상코인의 수익률 정보를 제공
     */
    private static TestAnalysisMulti.TotalYield getCoinYield(List<MabsMultiBacktestRow> tradeHistory, MabsMultiCondition condition) {
        List<Double> amountHistory = new ArrayList<>();
        amountHistory.add(tradeHistory.get(0).getFinalResult());
        amountHistory.addAll(tradeHistory.stream().skip(1).map(p -> p.getFinalResult()).collect(Collectors.toList()));
        TestAnalysisMulti.TotalYield totalYield = new TestAnalysisMulti.TotalYield();
        double realYield = tradeHistory.get(tradeHistory.size() - 1).getFinalResult() / tradeHistory.get(0).getFinalResult() - 1;
        double realMdd = ApplicationUtil.getMdd(amountHistory);
        totalYield.setYield(realYield);
        totalYield.setMdd(realMdd);

        // 승률
        for (MabsMultiBacktestRow row : tradeHistory) {
            if (row.getTradeEvent() != MabsMultiBacktestRow.TradeEvent.SELL) {
                continue;
            }
            if (row.getRealYield() > 0) {
                totalYield.setGainCount(totalYield.getGainCount() + 1);
            } else {
                totalYield.setLossCount(totalYield.getLossCount() + 1);
            }
        }
        long dayCount = condition.getRange().getDiffDays();
        totalYield.setDayCount((int) dayCount);
        return totalYield;
    }

    private void initMock(MabsMultiCondition condition, CandleDataProvider candleDataProvider) {
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
        when(accountService.getMyAccountBalance()).then((method) -> {
            Map<String, Account> map = accountMap.entrySet().stream()
                    .filter(e -> e.getValue().getBalanceValue() != 0)
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            return map;
        });


        // 시세 체크
        doAnswer(invocation -> {
            Candle currentCandle = invocation.getArgument(0);
            double maShort = invocation.getArgument(1, Double.class);
            double maLong = invocation.getArgument(2, Double.class);
            priceMap.put(currentCandle.getMarket(), new CurrentPrice(currentCandle, maShort, maLong));
            amountByCoin.get(currentCandle.getMarket()).add(currentCandle.getTradePrice());
            return null;
        }).when(tradeEvent).check(notNull(), anyDouble(), anyDouble());


        // 매수
        doAnswer(invocation -> {
            String market = invocation.getArgument(0, String.class);
            CurrentPrice currentPrice = priceMap.get(market);
            Candle candle = currentPrice.getCandle();
            MabsMultiBacktestRow backtestRow = new MabsMultiBacktestRow(candle);

            double tradePrice = invocation.getArgument(1);
            double bidPrice = tradePrice + condition.getTradeMargin();

            Account coinAccount = accountMap.get(market);
            coinAccount.setAvgBuyPrice(ApplicationUtil.toNumberString(bidPrice));
            double investAmount = invocation.getArgument(2);

            // 남은 현금 계산
            double fee = investAmount * condition.getFeeBid();
            Account krwAccount = accountMap.get("KRW");
            double cash = Double.parseDouble(krwAccount.getBalance()) - investAmount;
            double remainCash = cash - fee;
            krwAccount.setBalance(ApplicationUtil.toNumberString(remainCash));

            String balance = ApplicationUtil.toNumberString(investAmount / bidPrice);
            coinAccount.setBalance(balance);

            backtestRow.setTradeEvent(MabsMultiBacktestRow.TradeEvent.BUY);
            backtestRow.setBidPrice(bidPrice);
            backtestRow.setBuyAmount(investAmount);
            backtestRow.setBuyTotalAmount(getBuyTotalAmount(accountMap));
            backtestRow.setCash(cash);
            backtestRow.setFeePrice(fee);
            backtestRow.setMaShort(currentPrice.getMaShort());
            backtestRow.setMaLong(currentPrice.getMaLong());

            tradeHistory.add(backtestRow);

            return null;
        }).when(tradeEvent).bid(anyString(), anyDouble(), anyDouble());

        // 최고수익률
        doAnswer(invocation -> {
            String market = invocation.getArgument(0, String.class);
            double highYield = invocation.getArgument(1, Double.class);
            highYieldMap.put(market, highYield);

            return null;
        }).when(tradeEvent).highYield(anyString(), anyDouble());

        // 최저 수익률
        doAnswer(invocation -> {
            String market = invocation.getArgument(0, String.class);
            double lowYield = invocation.getArgument(1, Double.class);
            lowYieldMap.put(market, lowYield);
            return null;
        }).when(tradeEvent).lowYield(anyString(), anyDouble());

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
            double askPrice = tradePrice - condition.getTradeMargin();
            double askAmount = askPrice * balance;
            double fee = askAmount * condition.getFeeAsk();

            Account krwAccount = accountMap.get("KRW");
            double totalCash = Double.parseDouble(krwAccount.getBalance()) + askAmount - fee;
            krwAccount.setBalance(ApplicationUtil.toNumberString(totalCash));
            coinAccount.setBalance("0");
            coinAccount.setAvgBuyPrice(null);

            backtestRow.setBuyTotalAmount(getBuyTotalAmount(accountMap));
            backtestRow.setTradeEvent(MabsMultiBacktestRow.TradeEvent.SELL);
            backtestRow.setAskPrice(askPrice);
            backtestRow.setCash(krwAccount.getBalanceValue());
            backtestRow.setAskReason(invocation.getArgument(3));
            backtestRow.setFeePrice(backtestRow.getFeePrice() + fee);
            backtestRow.setMaShort(currentPrice.getMaShort());
            backtestRow.setMaLong(currentPrice.getMaLong());
            backtestRow.setHighYield(highYieldMap.getOrDefault(market, 0.0));
            backtestRow.setLowYield(lowYieldMap.getOrDefault(market, 0.0));

            tradeHistory.add(backtestRow);
            return null;
        }).when(tradeEvent).ask(anyString(), anyDouble(), anyDouble(), notNull());
    }

    /**
     * @param accountMap
     * @return 현재 투자한 코인 함
     */
    private double getBuyTotalAmount(Map<String, Account> accountMap) {
        double buyTotal = accountMap.entrySet().stream().filter(e -> !e.getKey().equals("KRW")).mapToDouble(e -> e.getValue().getInvestCash()).sum();
        return buyTotal;
    }


    private void injectionFieldValue(MabsMultiCondition condition) {
        ReflectionTestUtils.setField(mabsMultiService, "assetHistoryRepository", this.assetHistoryRepository);
        ReflectionTestUtils.setField(mabsMultiService, "tradeRepository", this.tradeRepository);
        ReflectionTestUtils.setField(mabsMultiService, "markets", condition.getMarkets());
        ReflectionTestUtils.setField(mabsMultiService, "maxBuyCount", condition.getMaxBuyCount());
        ReflectionTestUtils.setField(mabsMultiService, "investRatio", condition.getInvestRatio());
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

    private StringBuffer getReportRow(MabsMultiCondition condition, TestAnalysisMulti testAnalysis) {
        StringBuffer reportRow = new StringBuffer();
        reportRow.append(String.format("%s\t", condition.getRange()));
        reportRow.append(String.format("%s\t", condition.getTradePeriod()));
        reportRow.append(String.format("%s\t", condition.getMarkets()));
        reportRow.append(String.format("%,.2f%%\t", condition.getInvestRatio() * 100));
        reportRow.append(String.format("%d\t", condition.getMaxBuyCount()));
        reportRow.append(String.format("%,.0f\t", condition.getCash()));
        reportRow.append(String.format("%,.0f\t", condition.getTradeMargin()));
        reportRow.append(String.format("%,.2f%%\t", condition.getFeeBid() * 100));
        reportRow.append(String.format("%,.2f%%\t", condition.getFeeAsk() * 100));
        reportRow.append(String.format("%,.2f%%\t", condition.getUpBuyRate() * 100));
        reportRow.append(String.format("%,.2f%%\t", condition.getDownSellRate() * 100));
        reportRow.append(String.format("%,.2f%%\t", condition.getLoseStopRate() * 100));
        reportRow.append(String.format("%d\t", condition.getShortPeriod()));
        reportRow.append(String.format("%d\t", condition.getLongPeriod()));
        reportRow.append(String.format("%s\t", condition.getComment()));
        reportRow.append(String.format("%,.2f%%\t", testAnalysis.getTotal().getYield() * 100));
        reportRow.append(String.format("%,.2f%%\t", testAnalysis.getTotal().getMdd() * 100));
        reportRow.append(String.format("%d\t", testAnalysis.getTotal().getTradeCount()));
        reportRow.append(String.format("%,.2f%%\t", testAnalysis.getTotal().getWinRate() * 100));
        reportRow.append(String.format("%,.2f%%\t", testAnalysis.getTotal().getCagr() * 100));
        return reportRow;
    }


    public static void makeReport(MabsMultiCondition condition, List<MabsMultiBacktestRow> tradeHistory, TestAnalysisMulti testAnalysis) throws IOException {
        String header = "날짜(KST),날짜(UTC),코인,이벤트 유형,단기 이동평균, 장기 이동평균,매수 체결 가격,최고수익률,최저수익률,매도 체결 가격,매도 이유,실현 수익률,매수금액,전체코인 매수금액,현금,수수료,투자 수익(수수료포함),투자 결과,현금 + 전체코인 매수금액 - 수수료,수익비";
        StringBuilder report = new StringBuilder(header.replace(",", "\t")).append("\n");
        for (MabsMultiBacktestRow row : tradeHistory) {
            String dateKst = DateUtil.formatDateTime(row.getCandle().getCandleDateTimeKst());
            String dateUtc = DateUtil.formatDateTime(row.getCandle().getCandleDateTimeUtc());
            report.append(String.format("%s\t", dateKst));
            report.append(String.format("%s\t", dateUtc));
            report.append(String.format("%s\t", row.getCandle().getMarket()));
            report.append(String.format("%s\t", row.getTradeEvent()));
            report.append(String.format("%,.0f\t", row.getMaShort()));
            report.append(String.format("%,.0f\t", row.getMaLong()));
            report.append(String.format("%,.0f\t", row.getBidPrice()));
            report.append(String.format("%,.2f%%\t", row.getHighYield() * 100));
            report.append(String.format("%,.2f%%\t", row.getLowYield() * 100));
            report.append(String.format("%,.0f\t", row.getAskPrice()));
            report.append(String.format("%s\t", row.getAskReason() == null ? "" : row.getAskReason()));
            report.append(String.format("%,.2f%%\t", row.getRealYield() * 100));
            report.append(String.format("%,.0f\t", row.getBuyAmount()));
            report.append(String.format("%,.0f\t", row.getBuyTotalAmount()));
            report.append(String.format("%,.0f\t", row.getCash()));
            report.append(String.format("%,.0f\t", row.getFeePrice()));
            report.append(String.format("%,.0f\t", row.getGains()));
            report.append(String.format("%,.0f\t", row.getInvestResult()));
            report.append(String.format("%,.0f\t", row.getFinalResult()));
            report.append(String.format("%,.2f\n", row.getFinalResult() / condition.getCash()));
        }

        String coins = condition.getMarkets().stream().collect(Collectors.joining(","));
        String reportFileName = String.format("%s(%s ~ %s)_%s.txt",
                FilenameUtils.getBaseName(coins), condition.getRange().getFromString(), condition.getRange().getToString(), condition.getTradePeriod());
        report.append("\n-----------\n");
        for (String market : condition.getMarkets()) {
            TestAnalysisMulti.YieldMdd coinYield = testAnalysis.getCoinYield(market);
            report.append(String.format("[%s] 실제 수익\t %,.2f%%", market, coinYield.getYield() * 100)).append("\n");
            report.append(String.format("[%s] 실제 MDD\t %,.2f%%", market, coinYield.getMdd() * 100)).append("\n");
        }

        report.append("\n-----------\n");
        for (String market : condition.getMarkets()) {
            TestAnalysisMulti.CoinInvestment coinInvestment = testAnalysis.getCoinInvestment(market);
            report.append(String.format("[%s] 수익금액 합계\t %,.0f", market, coinInvestment.getInvest())).append("\n");
            report.append(String.format("[%s] 매매 횟수\t %d", market, coinInvestment.getTradeCount())).append("\n");
            report.append(String.format("[%s] 승률\t %,.2f%%", market, coinInvestment.getWinRate() * 100)).append("\n");
        }
        report.append("\n-----------\n");
        report.append(String.format("실현 수익\t %,.2f%%", testAnalysis.getTotal().getYield() * 100)).append("\n");
        report.append(String.format("실현 MDD\t %,.2f%%", testAnalysis.getTotal().getMdd() * 100)).append("\n");
        report.append(String.format("매매회수\t %d", testAnalysis.getTotal().getTradeCount())).append("\n");
        report.append(String.format("승률\t %,.2f%%", testAnalysis.getTotal().getWinRate() * 100)).append("\n");
        report.append(String.format("CAGR\t %,.2f%%", testAnalysis.getTotal().getCagr() * 100)).append("\n");

        report.append("\n-----------\n");
        report.append(String.format("분석기간\t %s", condition.getRange())).append("\n");
        report.append(String.format("분석주기\t %s", condition.getTradePeriod())).append("\n");
        report.append(String.format("대상 코인\t %s", condition.getMarkets())).append("\n");
        report.append(String.format("투자비율\t %,.2f%%", condition.getInvestRatio() * 100)).append("\n");
        report.append(String.format("최대 코인 매매 갯수\t %d", condition.getMaxBuyCount())).append("\n");
        report.append(String.format("최초 투자금액\t %,f", condition.getCash())).append("\n");
        report.append(String.format("매매 마진\t %,f", condition.getTradeMargin())).append("\n");
        report.append(String.format("매수 수수료\t %,.2f%%", condition.getFeeBid() * 100)).append("\n");
        report.append(String.format("매도 수수료\t %,.2f%%", condition.getFeeAsk() * 100)).append("\n");
        report.append(String.format("상승 매수률\t %,.2f%%", condition.getUpBuyRate() * 100)).append("\n");
        report.append(String.format("하락 매도률\t %,.2f%%", condition.getDownSellRate() * 100)).append("\n");
        report.append(String.format("단기 이동평균 기간\t %d", condition.getShortPeriod())).append("\n");
        report.append(String.format("장기 이동평균 기간\t %d", condition.getLongPeriod())).append("\n");
        report.append(String.format("손절\t %,.2f%%", condition.getLoseStopRate() * 100)).append("\n");

        File reportFile = new File("./backtest-result", reportFileName);
        FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr");
        System.out.println("결과 파일:" + reportFile.getName());
    }


    @RequiredArgsConstructor
    @Getter
    class CurrentPrice {
        final Candle candle;
        final double maShort;
        final double maLong;
    }


}
