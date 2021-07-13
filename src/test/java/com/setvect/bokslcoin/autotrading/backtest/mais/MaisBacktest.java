package com.setvect.bokslcoin.autotrading.backtest.mais;

import com.setvect.bokslcoin.autotrading.algorithm.AskReason;
import com.setvect.bokslcoin.autotrading.algorithm.BasicTradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.TradePeriod;
import com.setvect.bokslcoin.autotrading.algorithm.mais.MaisService;
import com.setvect.bokslcoin.autotrading.backtest.CandleDataIterator;
import com.setvect.bokslcoin.autotrading.backtest.TestAnalysis;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepository;
import com.setvect.bokslcoin.autotrading.exchange.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.OrderService;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class MaisBacktest {

    @Autowired
    private CandleRepository candleRepository;
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
    private MaisService maisService;

    private List<MaisBacktestRow> tradeHistory;

    @Test
    public void singleBacktest() throws IOException {
        // === 1. 변수값 설정 ===
        MaisCondition condition = MaisCondition.builder()
                .market("KRW-BTC")// 대상 코인
                .range(new DateRange("2021-06-17T00:00:00", "2021-07-07T23:59:59"))
//                .range(new DateRange("2021-06-17T00:00:00", "2021-07-02T23:59:59"))
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
                .investRatio(0.99) // 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
                .cash(10_000_000) // 최초 투자 금액
                .tradeMargin(1000)// 매매시 채결 가격 차이
                .feeBid(0.0005) //  매수 수수료
                .feeAsk(0.0005)//  매도 수수료
                .upBuyRate(0.005) //상승 매수율
                .downSellRate(0.005) // 하락 매도률
                .maPeriod(18) //  이동평균 기간
                .tradePeriod(TradePeriod.P_240) //매매 주기
                .build();

        // === 2. 백테스트 ===
        TestAnalysis testAnalysis = backtest(condition);
        System.out.println(condition.toString());
        System.out.printf("실제 수익: %,.2f%%\n", testAnalysis.getCoinYield() * 100);
        System.out.printf("실제 MDD: %,.2f%%\n", testAnalysis.getCoinMdd() * 100);
        System.out.printf("실현 수익: %,.2f%%\n", testAnalysis.getRealYield() * 100);
        System.out.printf("실현 MDD: %,.2f%%\n", testAnalysis.getRealMdd() * 100);
        System.out.printf("매매 횟수: %d\n", testAnalysis.getTradeCount());
        System.out.printf("승률: %,.2f%%\n", testAnalysis.getWinRate() * 100);
        System.out.printf("CAGR: %,.2f%%\n", testAnalysis.getCagr() * 100);

        // === 3. 리포트 ===
        makeReport(condition, tradeHistory, testAnalysis);

        System.out.println("끝");
    }


    @Test
    public void multiBacktest() throws IOException {
        String header = "분석기간,분석주기,대상 코인,투자비율,최초 투자금액,매매 마진,매수 수수료,매도 수수료,상승 매수률,하락 매도률,이동평균 기간,조건 설명,실제 수익,실제 MDD,실현 수익,실현 MDD,매매 횟수,승률,CAGR";


        StringBuffer report = new StringBuffer(header.replace(",", "\t") + "\n");
        MaisCondition condition;

        List<DateRange> rangeList = Arrays.asList(
                new DateRange("2020-11-01T00:00:00", "2021-04-14T23:59:59"), // 상승장
                new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59"), // 상승장 후 하락장
                new DateRange("2020-05-07T00:00:00", "2020-10-20T23:59:59"), // 횡보장1
                new DateRange("2020-05-08T00:00:00", "2020-07-26T23:59:59"), // 횡보장2
                new DateRange("2019-06-24T00:00:00", "2020-03-31T23:59:59"), // 횡보+하락장1
                new DateRange("2017-12-24T00:00:00", "2020-03-31T23:59:59"), // 횡보+하락장2
                new DateRange("2018-01-01T00:00:00", "2020-11-19T23:59:59"), // 횡보장3
                new DateRange("2021-04-14T00:00:00", "2021-06-08T23:59:59"), // 하락장1
                new DateRange("2017-12-07T00:00:00", "2018-02-06T23:59:59"), // 하락장2
                new DateRange("2018-01-06T00:00:00", "2018-02-06T23:59:59"), // 하락장3
                new DateRange("2018-01-06T00:00:00", "2018-12-15T23:59:59"), // 하락장4(찐하락장)
                new DateRange("2019-06-27T00:00:00", "2020-03-17T23:59:59"), // 하락장5
                new DateRange("2018-01-06T00:00:00", "2019-08-15T23:59:59"), // 하락장 이후 약간의 상승장
                new DateRange("2017-10-10T00:00:00", "2021-06-08T23:59:59") // 전체 기간
        );
        int count = 0;
        Date now = new Date();
        int[] maPeriod = {16, 17, 18, 19};
        for (int ma : maPeriod) {
            for (DateRange range : rangeList) {
                condition = MaisCondition.builder()
                        .market("KRW-BTC")// 대상 코인
                        .range(range)
                        .investRatio(0.99) // 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
                        .cash(10_000_000) // 최초 투자 금액
                        .tradeMargin(1000)// 매매시 채결 가격 차이
                        .feeBid(0.0005) //  매수 수수료
                        .feeAsk(0.0005)//  매도 수수료
                        .upBuyRate(0.005) //상승 매수율
                        .downSellRate(0.005) // 하락 매도률
                        .maPeriod(ma) //  이동평균 기간
                        .tradePeriod(TradePeriod.P_240) //매매 주기
                        .build();
                log.info(condition.toString());

                TestAnalysis testAnalysis;
                testAnalysis = backtest(condition);
                report.append(getReportRow(condition, testAnalysis) + "\n");
                makeReport(condition, tradeHistory, testAnalysis);

                // -- 결과 저장 --
                File reportFile = new File("./backtest-result", "이평선돌파_전략_백테스트_분석결과_" + (++count) + "_" + now.getTime() + ".txt");
                FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr");
                System.out.println("결과 파일:" + reportFile.getName());
            }
        }
        // -- 결과 저장 --
        File reportFile = new File("./backtest-result", "이평선돌파_전략_백테스트_분석결과_" + now.getTime() + ".txt");
        FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr");
        System.out.println("결과 파일:" + reportFile.getName());

        System.out.println("끝");
    }

    private TestAnalysis backtest(MaisCondition condition) {
        injectionFieldValue(condition);
        tradeHistory = new ArrayList<>();

        // TODO
        CandleDataIterator candleDataIterator = new CandleDataIterator(condition, candleRepository);
        initMock(condition, candleDataIterator);
        while (candleDataIterator.hasNext()) {
            candleDataIterator.next();
            maisService.apply();
        }

        // 맨 마지막에 매도가 이루어 지지 않으면 종가로 매도
        MaisBacktestRow lastBacktestRow = tradeHistory.get(tradeHistory.size() - 1);
        if (lastBacktestRow.getBidPrice() != 0) {
            lastBacktestRow.setAskPrice(lastBacktestRow.getCandle().getTradePrice());
            lastBacktestRow.setFeePrice(lastBacktestRow.getInvestmentAmount() * condition.getFeeAsk());
            lastBacktestRow.setAskReason(AskReason.TIME);
        }

        Mockito.reset(candleService, orderService, accountService, tradeEvent);
        return analysis(tradeHistory);
    }

    public static TestAnalysis analysis(List<MaisBacktestRow> tradeHistory) {
        TestAnalysis testAnalysis = new TestAnalysis();
        if (tradeHistory.isEmpty()) {
            return testAnalysis;
        }

        double coinYield = tradeHistory.get(tradeHistory.size() - 1).getCandle().getTradePrice() / tradeHistory.get(0).getCandle().getOpeningPrice() - 1;
        testAnalysis.setCoinYield(coinYield);
        List<Double> values = new ArrayList<>();
        values.add(tradeHistory.get(0).getCandle().getOpeningPrice());
        values.addAll(tradeHistory.stream().skip(1).map(p -> p.getCandle().getTradePrice()).collect(Collectors.toList()));
        testAnalysis.setCoinMdd(ApplicationUtil.getMdd(values));

        values = new ArrayList<>();
        values.add(tradeHistory.get(0).getFinalResult());
        values.addAll(tradeHistory.stream().skip(1).map(p -> p.getFinalResult()).collect(Collectors.toList()));
        testAnalysis.setRealMdd(ApplicationUtil.getMdd(values));

        double realYield = tradeHistory.get(tradeHistory.size() - 1).getFinalResult() / tradeHistory.get(0).getFinalResult() - 1;
        testAnalysis.setRealYield(realYield);

        // 승률
        for (MaisBacktestRow row : tradeHistory) {
            if (row.getAskReason() == null || row.getAskReason() == AskReason.SKIP) {
                continue;
            }
            if (row.getRealYield() > 0) {
                testAnalysis.setGainCount(testAnalysis.getGainCount() + 1);
            } else {
                testAnalysis.setLossCount(testAnalysis.getLossCount() + 1);
            }

        }

        //
        LocalDateTime from = tradeHistory.get(0).getCandle().getCandleDateTimeUtc();
        LocalDateTime to = tradeHistory.get(tradeHistory.size() - 1).getCandle().getCandleDateTimeUtc();
        long dayCount = ChronoUnit.DAYS.between(from, to);
        testAnalysis.setDayCount((int) dayCount);

        return testAnalysis;
    }

    private void initMock(MaisCondition condition, CandleDataIterator candleDataIterator) {
        when(candleService.getMinute(anyInt(), anyString()))
                .then((invocation) -> candleDataIterator.getCurrentCandle());

        when(candleService.getDay(anyString(), anyInt()))
                .then((invocation) -> candleDataIterator.beforeDayCandle(invocation.getArgument(1, Integer.class)));

        when(candleService.getMinute(eq(60), anyString(), anyInt()))
                .then((invocation) -> candleDataIterator.beforeMinute(PeriodType.PERIOD_60, invocation.getArgument(2, Integer.class)));
        when(candleService.getMinute(eq(240), anyString(), anyInt()))
                .then((invocation) -> candleDataIterator.beforeMinute(PeriodType.PERIOD_240, invocation.getArgument(2, Integer.class)));

        // AccountService
        Account krwAccount = new Account();
        Account coinAccount = new Account();
        krwAccount.setBalance(ApplicationUtil.toNumberString(condition.getCash()));
        when(accountService.getBalance(anyString())).then((method) -> {
            if (method.getArgument(0).equals("KRW")) {
                return BigDecimal.valueOf(Double.parseDouble(krwAccount.getBalance()));
            }
            return BigDecimal.valueOf(Double.parseDouble(coinAccount.getBalance()));
        });

        when(accountService.getAccount(anyString())).then((method) -> {
            if (method.getArgument(0).equals("KRW")) {
                return Optional.of(krwAccount);
            }
            return Optional.of(coinAccount);
        });
        AtomicReference<MaisBacktestRow> backtestInfoAtom = new AtomicReference<>();

        // 새로운 매매주기
        doAnswer(invocation -> {
            MaisBacktestRow beforeBacktestRow = backtestInfoAtom.get();
            Candle currentCandle = invocation.getArgument(0);
            MaisBacktestRow backtestRow = new MaisBacktestRow(currentCandle);

            if (beforeBacktestRow != null) {
                //  매수는 했는데 매도를 하지 않았을 경우
                if (beforeBacktestRow.getBidPrice() != 0 && beforeBacktestRow.getAskPrice() == 0) {
                    // 아직 매도하지 않았으므로 매도 채결가격은 매수 채결가격과 동일하게
                    beforeBacktestRow.setAskPrice(beforeBacktestRow.getBidPrice());
                    beforeBacktestRow.setAskReason(AskReason.SKIP);

                    backtestRow.setTrade(beforeBacktestRow.isTrade());
                    backtestRow.setBidPrice(beforeBacktestRow.getBidPrice());
                    backtestRow.setInvestmentAmount(beforeBacktestRow.getInvestmentAmount());
                }
                double beforeTradePrice = beforeBacktestRow.getCandle().getTradePrice();
                backtestRow.setBeforeTradePrice(beforeTradePrice);
            }

            backtestRow.setCash(Double.parseDouble(krwAccount.getBalance()));
            backtestInfoAtom.set(backtestRow);
            tradeHistory.add(backtestRow);

            log.debug("새로운 매매주기: {}", DateUtil.formatDateTime(currentCandle.getCandleDateTimeKst()));

            return null;
        }).when(tradeEvent).newPeriod(notNull());


        // 시세 체크
        doAnswer(invocation -> {
            Candle currentCandle = invocation.getArgument(0);
            MaisBacktestRow backtestRow = backtestInfoAtom.get();
            Candle candle = backtestRow.getCandle();
            candle.setLowPrice(Math.min(candle.getLowPrice(), currentCandle.getLowPrice()));
            candle.setHighPrice(Math.max(candle.getHighPrice(), currentCandle.getHighPrice()));
            candle.setTradePrice(currentCandle.getTradePrice());
            backtestRow.setHighYield(Math.max(backtestRow.getHighYield(), maisService.getHighYield()));
            backtestRow.setCurrentMa(invocation.getArgument(1));
            backtestRow.setMaxMa(invocation.getArgument(2));
            backtestRow.setMinMa(invocation.getArgument(3));
            return null;
        }).when(tradeEvent).check(notNull(), anyDouble(), anyDouble(), anyDouble());

        // 매수
        doAnswer(invocation -> {
            MaisBacktestRow backtestRow = backtestInfoAtom.get();
            double tradePrice = invocation.getArgument(1);
            double bidPrice = tradePrice + condition.getTradeMargin();
            // 매수가를 매수 목표가로 즉 호가창 이동 없이 바로 잡았다고 가정
//            double bidPrice = backtestRow.getTargetPrice() + condition.getTradeMargin();
            coinAccount.setAvgBuyPrice(ApplicationUtil.toNumberString(bidPrice));
            double investAmount = invocation.getArgument(2);

            // 남은 현금 계산
            double fee = investAmount * condition.getFeeBid();
            double cash = Double.parseDouble(krwAccount.getBalance()) - investAmount;
            double remainCash = cash - fee;
            krwAccount.setBalance(ApplicationUtil.toNumberString(remainCash));

            String balance = ApplicationUtil.toNumberString(investAmount / bidPrice);
            coinAccount.setBalance(balance);

            backtestRow.setTrade(true);
            backtestRow.setBidPrice(bidPrice);
            backtestRow.setInvestmentAmount(investAmount);
            backtestRow.setCash(cash);
            backtestRow.setFeePrice(fee);
            return null;
        }).when(tradeEvent).bid(anyString(), anyDouble(), anyDouble());

        // 매도
        doAnswer(invocation -> {
            double tradePrice = invocation.getArgument(2);
            double balance = Double.parseDouble(coinAccount.getBalance());
            double askPrice = tradePrice - condition.getTradeMargin();
            double askAmount = askPrice * balance;
            double fee = askAmount * condition.getFeeAsk();

            double totalCash = Double.parseDouble(krwAccount.getBalance()) + askAmount - fee;
            krwAccount.setBalance(ApplicationUtil.toNumberString(totalCash));
            coinAccount.setBalance("0");
            coinAccount.setAvgBuyPrice(null);

            MaisBacktestRow backtestRow = backtestInfoAtom.get();
            backtestRow.setAskPrice(askPrice);
            backtestRow.setAskReason(invocation.getArgument(3));
            backtestRow.setFeePrice(backtestRow.getFeePrice() + fee);
            return null;
        }).when(tradeEvent).ask(anyString(), anyDouble(), anyDouble(), notNull());
    }


    private void injectionFieldValue(MaisCondition condition) {
        ReflectionTestUtils.setField(maisService, "market", condition.getMarket());
        ReflectionTestUtils.setField(maisService, "investRatio", condition.getInvestRatio());
        ReflectionTestUtils.setField(maisService, "upBuyRate", condition.getUpBuyRate());
        ReflectionTestUtils.setField(maisService, "downSellRate", condition.getDownSellRate());
        ReflectionTestUtils.setField(maisService, "tradePeriod", condition.getTradePeriod());
        ReflectionTestUtils.setField(maisService, "maPeriod", condition.getMaPeriod());
        ReflectionTestUtils.setField(maisService, "periodIdx", -1);
        ReflectionTestUtils.setField(maisService, "slackTime", "08:00");
    }

    private StringBuffer getReportRow(MaisCondition condition, TestAnalysis testAnalysis) {
        StringBuffer reportRow = new StringBuffer();
        reportRow.append(String.format("%s\t", condition.getRange()));
        reportRow.append(String.format("%s\t", condition.getTradePeriod()));
        reportRow.append(String.format("%s\t", condition.getMarket()));
        reportRow.append(String.format("%,.2f%%\t", condition.getInvestRatio() * 100));
        reportRow.append(String.format("%,.0f\t", condition.getCash()));
        reportRow.append(String.format("%,.0f\t", condition.getTradeMargin()));
        reportRow.append(String.format("%,.2f%%\t", condition.getFeeBid() * 100));
        reportRow.append(String.format("%,.2f%%\t", condition.getFeeAsk() * 100));
        reportRow.append(String.format("%,.2f%%\t", condition.getUpBuyRate() * 100));
        reportRow.append(String.format("%,.2f%%\t", condition.getDownSellRate() * 100));
        reportRow.append(String.format("%d\t", condition.getMaPeriod()));
        reportRow.append(String.format("%s\t", condition.getComment()));
        reportRow.append(String.format("%,.2f%%\t", testAnalysis.getCoinYield() * 100));
        reportRow.append(String.format("%,.2f%%\t", testAnalysis.getCoinMdd() * 100));
        reportRow.append(String.format("%,.2f%%\t", testAnalysis.getRealYield() * 100));
        reportRow.append(String.format("%,.2f%%\t", testAnalysis.getRealMdd() * 100));
        reportRow.append(String.format("%d\t", testAnalysis.getTradeCount()));
        reportRow.append(String.format("%,.2f%%\t", testAnalysis.getWinRate() * 100));
        reportRow.append(String.format("%,.2f%%\t", testAnalysis.getCagr() * 100));
        return reportRow;
    }


    public static void makeReport(MaisCondition condition, List<MaisBacktestRow> tradeHistory, TestAnalysis testAnalysis) throws IOException {
        String header = "날짜(KST),날짜(UTC),시가,고가,저가,종가,직전 종가,단위 수익률,이동평균,최대 이동평균,최소 이동평균,매매여부,매수 체결 가격,최고수익률,매도 체결 가격,매도 이유,실현 수익률,투자금,현금,투자 수익,수수료,투자 결과,현금 + 투자결과 - 수수료";
        StringBuilder report = new StringBuilder(header.replace(",", "\t")).append("\n");
        for (MaisBacktestRow row : tradeHistory) {
            String dateKst = DateUtil.formatDateTime(row.getCandle().getCandleDateTimeKst());
            String dateUtc = DateUtil.formatDateTime(row.getCandle().getCandleDateTimeUtc());
            report.append(String.format("%s\t", dateKst));
            report.append(String.format("%s\t", dateUtc));
            report.append(String.format("%,.0f\t", row.getCandle().getOpeningPrice()));
            report.append(String.format("%,.0f\t", row.getCandle().getHighPrice()));
            report.append(String.format("%,.0f\t", row.getCandle().getLowPrice()));
            report.append(String.format("%,.0f\t", row.getCandle().getTradePrice()));
            report.append(String.format("%,.0f\t", row.getBeforeTradePrice()));
            report.append(String.format("%,.2f%%\t", row.getCandleYield() * 100));
            report.append(String.format("%,.0f\t", row.getCurrentMa()));
            report.append(String.format("%,.0f\t", row.getMaxMa()));
            report.append(String.format("%,.0f\t", row.getMinMa()));
            report.append(String.format("%s\t", row.isTrade()));
            report.append(String.format("%,.0f\t", row.getBidPrice()));
            report.append(String.format("%,.2f%%\t", row.getHighYield() * 100));
            report.append(String.format("%,.0f\t", row.getAskPrice()));
            report.append(String.format("%s\t", row.getAskReason() == null ? "" : row.getAskReason()));
            report.append(String.format("%,.2f%%\t", row.getRealYield() * 100));
            report.append(String.format("%,.0f\t", row.getInvestmentAmount()));
            report.append(String.format("%,.0f\t", row.getCash()));
            report.append(String.format("%,.0f\t", row.getGains()));
            report.append(String.format("%,.0f\t", row.getFeePrice()));
            report.append(String.format("%,.0f\t", row.getInvestResult()));
            report.append(String.format("%,.0f\n", row.getFinalResult()));
        }

        String reportFileName = String.format("%s(%s ~ %s)_%s.txt",
                FilenameUtils.getBaseName(condition.getMarket()), condition.getRange().getFromString(), condition.getRange().getToString(), condition.getTradePeriod());
        report.append("\n\n-----------\n");
        report.append(String.format("실제 수익\t %,.2f%%", testAnalysis.getCoinYield() * 100)).append("\n");
        report.append(String.format("실제 MDD\t %,.2f%%", testAnalysis.getCoinMdd() * 100)).append("\n");
        report.append(String.format("실현 수익\t %,.2f%%", testAnalysis.getRealYield() * 100)).append("\n");
        report.append(String.format("실현 MDD\t %,.2f%%", testAnalysis.getRealMdd() * 100)).append("\n");
        report.append(String.format("매매회수\t %d", testAnalysis.getTradeCount())).append("\n");
        report.append(String.format("승률\t %,.2f%%", testAnalysis.getWinRate() * 100)).append("\n");
        report.append(String.format("CAGR\t %,.2f%%", testAnalysis.getCagr() * 100)).append("\n");

        report.append("\n\n-----------\n");
        report.append(String.format("분석기간\t %s", condition.getRange())).append("\n");
        report.append(String.format("분석주기\t %s", condition.getTradePeriod())).append("\n");
        report.append(String.format("대상 코인\t %s", condition.getMarket())).append("\n");
        report.append(String.format("투자비율\t %,.2f%%", condition.getInvestRatio() * 100)).append("\n");
        report.append(String.format("최초 투자금액\t %,f", condition.getCash())).append("\n");
        report.append(String.format("매매 마진\t %,f", condition.getTradeMargin())).append("\n");
        report.append(String.format("매수 수수료\t %,.2f%%", condition.getFeeBid() * 100)).append("\n");
        report.append(String.format("매도 수수료\t %,.2f%%", condition.getFeeAsk() * 100)).append("\n");
        report.append(String.format("상승 매수률\t %,.2f%%", condition.getUpBuyRate() * 100)).append("\n");
        report.append(String.format("하락 매도률\t %,.2f%%", condition.getDownSellRate() * 100)).append("\n");
        report.append(String.format("이동평균 기간\t %d", condition.getMaPeriod())).append("\n");

        File reportFile = new File("./backtest-result", reportFileName);
        FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr");
        System.out.println("결과 파일:" + reportFile.getName());
    }


}
