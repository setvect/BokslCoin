package com.setvect.bokslcoin.autotrading.backtest.vbsstop;

import com.google.gson.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.algorithm.BasicTradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.VbsStopService;
import com.setvect.bokslcoin.autotrading.backtest.TestAnalysis;
import com.setvect.bokslcoin.autotrading.exchange.service.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.service.OrderService;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.service.CandleService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;


@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class VbsStopBacktest1 {
    @Mock
    private AccountService accountService;

    @Mock
    private CandleService candleService;

    @Mock
    private OrderService orderService;

    @Spy
    private TradeEvent tradeEvent = new BasicTradeEvent();

    @InjectMocks
    private VbsStopService vbsStopService;

    private List<VbsStopBacktestRow> tradeHistory;

    @Test
    public void singleBacktest() throws IOException {
        // === 1. 변수값 설정 ===
        VbsStopCondition condition = VbsStopCondition.builder()
                .k(0.5) // 변동성 돌파 판단 비율
                .investRatio(0.5) // 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
                .range(new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59"))// 분석 대상 기간 (UTC)
                .market("KRW-BTC")// 대상 코인
                .cash(10_000_000) // 최초 투자 금액
                .tradeMargin(1_000)// 매매시 채결 가격 차이
                .feeBid(0.0005) //  매수 수수료
                .feeAsk(0.0005)//  매도 수수료
                .loseStopRate(0.03) // 손절 라인
                .gainStopRate(0.50) //익절 라인
                .tradePeriod(VbsStopService.TradePeriod.P_1440) //매매 주기
                .build();

        injectionFieldValue(condition);
        // === 2. 백테스트 ===
        TestAnalysis testAnalysis = backtest(condition);
        System.out.printf("실제 수익: %,.2f%%\n", testAnalysis.getCoinYield() * 100);
        System.out.printf("실제 MDD: %,.2f%%\n", testAnalysis.getCoinMdd() * 100);
        System.out.printf("실현 수익: %,.2f%%\n", testAnalysis.getRealYield() * 100);
        System.out.printf("실현 MDD: %,.2f%%\n", testAnalysis.getRealMdd() * 100);

        // === 3. 리포트 ===
        VbsStopUtil.makeReport(condition, tradeHistory, testAnalysis);

        System.out.println("끝");
    }

    @Test
    public void multiBacktest() throws IOException {
        String header = "분석기간,분석주기,대상 코인,변동성 비율(K),투자비율,최초 투자금액,매매 마진,매수 수수료,매도 수수료,손절,익절,조건 설명,실제 수익,실제 MDD,실현 수익,실현 MDD";

        StringBuffer report = new StringBuffer(header.replace(",", "\t") + "\n");
        VbsStopCondition condition;

        List<DateRange> rangeList = Arrays.asList(
                new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59"),
                new DateRange("2020-11-01T00:00:00", "2021-04-14T23:59:59"),
                new DateRange("2021-04-14T00:00:00", "2021-06-08T23:59:59"),
                new DateRange("2020-05-07T00:00:00", "2020-10-20T23:59:59"),
                new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59")
        );

        for (DateRange range : rangeList) {
            for (double gainStopRate = 0.01; gainStopRate <= 0.20; gainStopRate = Math.round((gainStopRate + 0.01) * 100.0) / 100.0) {
                condition = VbsStopCondition.builder()
                        .k(0.5) // 변동성 돌파 판단 비율
                        .investRatio(0.5) // 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
                        .range(range)// 분석 대상 기간 (UTC)
                        .market("KRW-BTC")// 대상 코인
                        .cash(10_000_000) // 최초 투자 금액
                        .tradeMargin(1_000)// 매매시 채결 가격 차이
                        .feeBid(0.0005) //  매수 수수료
                        .feeAsk(0.0005)//  매도 수수료
                        .loseStopRate(0.01) // 손절 라인
                        .gainStopRate(gainStopRate) //익절 라인
                        .tradePeriod(VbsStopService.TradePeriod.P_1440) //매매 주기
                        .comment("-")
                        .build();
                log.info(condition.toString());

                TestAnalysis testAnalysis;
                testAnalysis = backtest(condition);
                report.append(getReportRow(condition, testAnalysis) + "\n");
            }
        }


        // -- 결과 저장 --
        File reportFile = new File("./backtest-result", "변동성돌파,손절,익절_전략_백테스트_분석결과.txt");
        FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr");
        System.out.println("결과 파일:" + reportFile.getName());

        System.out.println("끝");
    }

    private StringBuffer getReportRow(VbsStopCondition condition, TestAnalysis testAnalysis) {
        StringBuffer reportRow = new StringBuffer();
        reportRow.append(String.format("%s\t", condition.getRange()));
        reportRow.append(String.format("%s\t", condition.getTradePeriod()));
        reportRow.append(String.format("%s\t", condition.getMarket()));
        reportRow.append(String.format("%,.2f\t", condition.getK()));
        reportRow.append(String.format("%,.2f%%\t", condition.getInvestRatio() * 100));
        reportRow.append(String.format("%,.0f\t", condition.getCash()));
        reportRow.append(String.format("%,.0f\t", condition.getTradeMargin()));
        reportRow.append(String.format("%,.2f%%\t", condition.getFeeBid() * 100));
        reportRow.append(String.format("%,.2f%%\t", condition.getFeeAsk() * 100));
        reportRow.append(String.format("%,.2f%%\t", condition.getLoseStopRate() * 100));
        reportRow.append(String.format("%,.2f%%\t", condition.getGainStopRate() * 100));
        reportRow.append(String.format("%s\t", condition.getComment()));
        reportRow.append(String.format("%,.2f%%\t", testAnalysis.getCoinYield() * 100));
        reportRow.append(String.format("%,.2f%%\t", testAnalysis.getCoinMdd() * 100));
        reportRow.append(String.format("%,.2f%%\t", testAnalysis.getRealYield() * 100));
        reportRow.append(String.format("%,.2f%%\t", testAnalysis.getRealMdd() * 100));
        return reportRow;
    }

    private TestAnalysis backtest(VbsStopCondition condition) {
        injectionFieldValue(condition);
        tradeHistory = new ArrayList<>();

        CandleDataIterator candleDataIterator = initMock(condition);
        while (candleDataIterator.hasNext()) {
            vbsStopService.apply();
        }
        Mockito.reset(candleService, orderService, accountService, tradeEvent);
        return VbsStopUtil.analysis(tradeHistory);
    }


    private CandleDataIterator initMock(VbsStopCondition condition) {
        File dataDir = new File("./craw-data/minute");
        CandleDataIterator candleDataIterator = new CandleDataIterator(dataDir, condition);
        // CandleService
        when(candleService.getMinute(anyInt(), anyString())).then((invocation) -> {
            if (candleDataIterator.hasNext()) {
                return candleDataIterator.next();
            }
            return null;
        });

        when(candleService.getDay(anyString(), eq(2))).then((invocation) -> candleDataIterator.beforeDayCandle());
        when(candleService.getMinute(eq(60), anyString(), eq(2))).then((invocation) -> candleDataIterator.before60Minute());
        when(candleService.getMinute(eq(240), anyString(), eq(2))).then((invocation) -> candleDataIterator.before240Minute());

        // AccountService
        Account krwAccount = new Account();
        Account coinAccount = new Account();
        krwAccount.setBalance(ApplicationUtil.toNumberString(condition.getCash()));
        when(accountService.getBalance(anyString())).then((method) -> {
            if (method.getArgument(0).equals("KRW")) {
                return BigDecimal.valueOf(Double.valueOf(krwAccount.getBalance()));
            }
            return BigDecimal.valueOf(Double.valueOf(coinAccount.getBalance()));
        });

        when(accountService.getAccount(anyString())).then((method) -> {
            if (method.getArgument(0).equals("KRW")) {
                return Optional.of(krwAccount);
            }
            return Optional.of(coinAccount);
        });
        AtomicReference<VbsStopBacktestRow> backtestInfoAtom = new AtomicReference<>();

        // 새로운 매매주기
        doAnswer(invocation -> {
            Candle currentCandle = invocation.getArgument(0);
            VbsStopBacktestRow backtestRow = new VbsStopBacktestRow(currentCandle);
            backtestRow.setCash(Double.parseDouble(krwAccount.getBalance()));
            backtestInfoAtom.set(backtestRow);
            tradeHistory.add(backtestRow);
            if (tradeHistory.size() >= 2) {
                VbsStopBacktestRow beforeBacktestRow = tradeHistory.get(tradeHistory.size() - 2);
                double beforeTradePrice = beforeBacktestRow.getCandle().getTradePrice();
                backtestRow.setBeforeTradePrice(beforeTradePrice);
            }

            log.debug("새로운 매매주기: {}", DateUtil.formatDateTime(currentCandle.getCandleDateTimeKst()));
            return null;
        }).when(tradeEvent).newPeriod(notNull());


        // 시황 체크
        doAnswer(invocation -> {
            Candle currentCandle = invocation.getArgument(0);
            VbsStopBacktestRow backtestRow = backtestInfoAtom.get();
            Candle candle = backtestRow.getCandle();
            candle.setLowPrice(Math.min(candle.getLowPrice(), currentCandle.getLowPrice()));
            candle.setHighPrice(Math.max(candle.getHighPrice(), currentCandle.getHighPrice()));
            candle.setTradePrice(currentCandle.getTradePrice());

            return null;
        }).when(tradeEvent).check(notNull());

        doAnswer(invocation -> {
            double targetPrice = invocation.getArgument(0);
            VbsStopBacktestRow backtestRow = backtestInfoAtom.get();
            backtestRow.setTargetPrice(targetPrice);
            return null;
        }).when(tradeEvent).registerTargetPrice(anyDouble());

        // 매수
        doAnswer(invocation -> {
            VbsStopBacktestRow backtestRow = backtestInfoAtom.get();
            double tradePrice = invocation.getArgument(1);
            double bidPrice = tradePrice + condition.getTradeMargin();
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
            VbsStopService.AskReason reason = invocation.getArgument(3);
            double tradePrice = invocation.getArgument(2);
            double balance = Double.parseDouble(coinAccount.getBalance());
            double askPrice = tradePrice - condition.getTradeMargin();
            double cash = askPrice * balance;
            double fee = cash * condition.getFeeAsk();

            double totalCash = Double.parseDouble(krwAccount.getBalance()) + cash - fee;
            krwAccount.setBalance(ApplicationUtil.toNumberString(totalCash));
            coinAccount.setBalance("0");
            coinAccount.setAvgBuyPrice(null);

            VbsStopBacktestRow backtestRow = backtestInfoAtom.get();
            backtestRow.setAskPrice(askPrice);
            backtestRow.setAskReason(invocation.getArgument(3));
            backtestRow.setFeePrice(backtestRow.getFeePrice() + fee);
            return null;
        }).when(tradeEvent).ask(anyString(), anyDouble(), anyDouble(), notNull());
        return candleDataIterator;
    }

    private void injectionFieldValue(VbsStopCondition condition) {
        ReflectionTestUtils.setField(vbsStopService, "market", condition.getMarket());
        ReflectionTestUtils.setField(vbsStopService, "k", condition.getK());
        ReflectionTestUtils.setField(vbsStopService, "investRatio", condition.getInvestRatio());
        ReflectionTestUtils.setField(vbsStopService, "loseStopRate", condition.getLoseStopRate());
        ReflectionTestUtils.setField(vbsStopService, "gainStopRate", condition.getGainStopRate());
        ReflectionTestUtils.setField(vbsStopService, "tradePeriod", condition.getTradePeriod());
        ReflectionTestUtils.setField(vbsStopService, "periodIdx", -1);
    }

    class CandleDataIterator implements Iterator<CandleMinute> {
        private final File dataDir;
        private final VbsStopCondition condition;
        private Iterator<CandleMinute> currentCandleIterator;
        private LocalDateTime bundleDate;
        private LocalDateTime currentUtc;

        private Queue<Candle> beforeData = new CircularFifoQueue<>(60 * 24 * 2);

        public CandleDataIterator(File dataDir, VbsStopCondition condition) {
            this.dataDir = dataDir;
            this.condition = condition;
            bundleDate = condition.getRange().getFrom();
            this.currentCandleIterator = Collections.emptyIterator();
        }

        @Override
        public boolean hasNext() {
            // 현재
            boolean exist = currentCandleIterator.hasNext();
            if (!exist) {
                List<CandleMinute> candleList = nextBundle();
                this.currentCandleIterator = candleList.iterator();
            }
            return this.currentCandleIterator.hasNext();
        }

        @Override
        public CandleMinute next() {
            if (hasNext()) {
                CandleMinute currentCandleMinute = currentCandleIterator.next();
                beforeData.add(currentCandleMinute);
                currentUtc = currentCandleMinute.getCandleDateTimeUtc();
                return currentCandleMinute;
            }
            throw new NoSuchElementException();
        }

        @SneakyThrows
        private List<CandleMinute> nextBundle() {
            String dataFileName = String.format("%s-minute(%s).json", condition.getMarket(), DateUtil.format(bundleDate, "yyyy-MM"));
            File dataFile = new File(dataDir, dataFileName);
            if (!dataFile.exists()) {
                log.warn("no exist file: {}", dataFile.getAbsolutePath());
                return Collections.emptyList();
            }
            List<CandleMinute> candles = GsonUtil.GSON.fromJson(FileUtils.readFileToString(dataFile, "utf-8"), new TypeToken<List<CandleMinute>>() {
            }.getType());
            log.info(String.format("load data file: %s", dataFileName));

            List<CandleMinute> candleFiltered = candles.stream().filter(p -> condition.getRange().isBetween(p.getCandleDateTimeUtc())).collect(Collectors.toList());
            // 과거 데이터를 먼저(날짜 기준 오름 차순 정렬)
            Collections.reverse(candleFiltered);

            // 다음달 가르킴
            bundleDate = bundleDate.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).plusMonths(1);
            return candleFiltered;
        }

        public List<CandleDay> beforeDayCandle() {
            LocalDateTime from = currentUtc.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime to = from.plusDays(1).minusNanos(1);
            DateRange range = new DateRange(from, to);
            List<Candle> filtered = beforeData.stream().filter(p -> range.isBetween(p.getCandleDateTimeUtc())).collect(Collectors.toList());
            // 수집이 안된 시간도 있음(업비트 오류로 판단)
            // 그래서 분봉 데이터가 일정 갯수 이상 되면 계산
            if (filtered.size() < VbsStopService.TradePeriod.P_1440.getTotal() / 2) {
                return Arrays.asList(null, null);
            }
            CandleDay period = new CandleDay();
            Candle first = filtered.get(0);
            Candle last = filtered.get(filtered.size() - 1);
            period.setOpeningPrice(first.getOpeningPrice());
            period.setCandleDateTimeUtc(first.getCandleDateTimeUtc());
            period.setCandleDateTimeKst(first.getCandleDateTimeKst());
            period.setTradePrice(last.getTradePrice());
            double low = filtered.stream().mapToDouble(p -> p.getLowPrice()).min().getAsDouble();
            period.setLowPrice(low);
            double high = filtered.stream().mapToDouble(p -> p.getHighPrice()).max().getAsDouble();
            period.setHighPrice(high);
            return Arrays.asList(null, period);
        }

        public List<CandleMinute> before60Minute() {
            LocalDateTime from = currentUtc.minusHours(1).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime to = from.plusHours(1).minusNanos(1);
            return getCandleMinutes(from, to);
        }

        public List<CandleMinute> before240Minute() {
            int diffHour = currentUtc.getHour() % 4 + 4;
            LocalDateTime from = currentUtc.minusHours(diffHour).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime to = from.plusHours(4).minusNanos(1);
            return getCandleMinutes(from, to);
        }

        private List<CandleMinute> getCandleMinutes(LocalDateTime from, LocalDateTime to) {
            Duration duration = Duration.between(from, to);
            long diffMinute = duration.getSeconds() / 60 + 1;

            DateRange range = new DateRange(from, to);
            List<Candle> filtered = beforeData.stream().filter(p -> range.isBetween(p.getCandleDateTimeUtc())).collect(Collectors.toList());
            // 수집이 안된 시간도 있음(업비트 오류로 판단)
            // 그래서 분봉 데이터가 일정 갯수 이상 되면 계산
            if (filtered.size() < diffMinute / 2) {
                return Arrays.asList(null, null);
            }
            CandleMinute period = new CandleMinute();
            Candle first = filtered.get(0);
            Candle last = filtered.get(filtered.size() - 1);
            period.setOpeningPrice(first.getOpeningPrice());
            period.setCandleDateTimeUtc(first.getCandleDateTimeUtc());
            period.setCandleDateTimeKst(first.getCandleDateTimeKst());
            period.setTradePrice(last.getTradePrice());
            double low = filtered.stream().mapToDouble(p -> p.getLowPrice()).min().getAsDouble();
            period.setLowPrice(low);
            double high = filtered.stream().mapToDouble(p -> p.getHighPrice()).max().getAsDouble();
            period.setHighPrice(high);
            return Arrays.asList(null, period);
        }
    }
}
