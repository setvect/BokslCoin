package com.setvect.bokslcoin.autotrading.backtest.vbsstop;

import com.google.gson.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.VbsStopService;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
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
    @Mock
    private TradeEvent tradeEvent;
    @InjectMocks
    private VbsStopService vbsStopService;

    @Test
    public void backtest() throws InterruptedException, IOException {
        // === 1. 변수값 설정 ===
        VbsStopCondition condition = VbsStopCondition.builder()
                .k(0.5) // 변동성 돌파 판단 비율
                .investRatio(0.5) // 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
                .range(new DateRange("2021-01-01T00:00:00", "2021-01-07T23:59:59"))// 분석 대상 기간
                .market("KRW-BTC")// 대상 코인
                .cash(10_000_000) // 최초 투자 금액
                .tradeMargin(1_000)// 매매시 채결 가격 차이
                .feeBid(0.0005) //  매수 수수료
                .feeAsk(0.0005)//  매도 수수료
                .loseStopRate(0.05) // 손절 라인
                .gainStopRate(0.1) //익절 라인
                .tradePeriod(VbsStopService.TradePeriod.P_1440) //매매 주기
                .build();

        ReflectionTestUtils.setField(vbsStopService, "market", condition.getMarket());
        ReflectionTestUtils.setField(vbsStopService, "k", condition.getK());
        ReflectionTestUtils.setField(vbsStopService, "gainRate", condition.getGainStopRate());
        ReflectionTestUtils.setField(vbsStopService, "loseStopRate", condition.getLoseStopRate());
        ReflectionTestUtils.setField(vbsStopService, "gainStopRate", condition.getGainStopRate());
        ReflectionTestUtils.setField(vbsStopService, "tradePeriod", condition.getTradePeriod());


        // === 2. Mock 만들기 ===
        // candleService
        File dataDir = new File("./craw-data/minute");
        CandleDataIterator candleDataIterator = new CandleDataIterator(dataDir, condition);
        when(candleService.getMinute(anyInt(), anyString())).then((method) -> {
            if (candleDataIterator.hasNext()) {
                return candleDataIterator.next();
            }
            return null;
        });

        when(candleService.getDay(anyString(), eq(2))).then((method) -> candleDataIterator.beforeDayCandle());
        when(candleService.getMinute(eq(60), anyString(), eq(2))).then((method) -> candleDataIterator.before60Minute());
        when(candleService.getMinute(eq(240), anyString(), eq(2))).then((method) -> candleDataIterator.before240Minute());

        // AccountService
        Account krwAccount = new Account();
        krwAccount.setBalance(ApplicationUtil.toNumberString(condition.getCash()));
        when(accountService.getBalance(eq("KRW"))).then((method) -> {
            return BigDecimal.valueOf(Double.valueOf(krwAccount.getBalance()));
        });

        when(accountService.getAccount(eq("KRW"))).then((method) -> {
            return Optional.of(krwAccount);
        });

        // === 3. 백테스팅 ===
        while (candleDataIterator.hasNext()) {
            vbsStopService.apply();
        }


        System.out.println("끝");
    }

    class CandleDataIterator implements Iterator<CandleMinute> {
        private final File dataDir;
        private final VbsStopCondition condition;
        private Iterator<CandleMinute> currentCandleIterator;
        private List<CandleMinute> currentCandleBundle;
        private LocalDateTime bundleDate;
        private LocalDateTime current;

        private Queue<Candle> beforeData = new CircularFifoQueue<>(60 * 24 * 2);

        public CandleDataIterator(File dataDir, VbsStopCondition condition) throws IOException {
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
                CandleMinute next = currentCandleIterator.next();
                beforeData.add(next);
                current = next.getCandleDateTimeUtc();
                return next;
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
            log.info(String.format("load data file: %s%n", dataFileName));

            List<CandleMinute> candleFiltered = candles.stream().filter(p -> condition.getRange().isBetween(p.getCandleDateTimeUtc())).collect(Collectors.toList());
            // 과거 데이터를 먼저(날짜 기준 오름 차순 정렬)
            Collections.reverse(candleFiltered);

            // 다음달 가르킴
            bundleDate = bundleDate.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).plusMonths(1);
            return candleFiltered;
        }

        public List<CandleDay> beforeDayCandle() {
            LocalDateTime from = current.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime to = from.plusDays(1).minusNanos(1);
            DateRange range = new DateRange(from, to);

            List<Candle> filtered = beforeData.stream().filter(p -> range.isBetween(p.getCandleDateTimeUtc())).collect(Collectors.toList());
            System.out.println(filtered.size() + ", " + beforeData.size());
            // 하루가 다 안차면 빈값 반환
            if (filtered.size() != 1440) {
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
            double high = filtered.stream().mapToDouble(p -> p.getHighPrice()).min().getAsDouble();
            period.setHighPrice(high);
            return Arrays.asList(null, period);
        }

        public List<CandleMinute> before60Minute() {
            LocalDateTime from = bundleDate.minusHours(1).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime to = from.plusHours(1).minusNanos(1);
            return getCandleMinutes(from, to);
        }

        public List<CandleMinute> before240Minute() {
            int diffHour = bundleDate.getHour() % 4 + 4;
            LocalDateTime from = bundleDate.minusHours(diffHour).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime to = from.plusHours(4).minusNanos(1);
            return getCandleMinutes(from, to);
        }

        private List<CandleMinute> getCandleMinutes(LocalDateTime from, LocalDateTime to) {
            Duration duration = Duration.between(from, to);
            long diffMinute = duration.getSeconds() % 60;

            DateRange range = new DateRange(from, to);
            List<Candle> filtered = beforeData.stream().filter(p -> range.isBetween(p.getCandleDateTimeUtc())).collect(Collectors.toList());
            // 한시간이 다 안차면 빈값 반환
            if (filtered.size() != diffMinute) {
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
            double high = filtered.stream().mapToDouble(p -> p.getHighPrice()).min().getAsDouble();
            period.setHighPrice(high);
            return Arrays.asList(null, period);
        }
    }
}
