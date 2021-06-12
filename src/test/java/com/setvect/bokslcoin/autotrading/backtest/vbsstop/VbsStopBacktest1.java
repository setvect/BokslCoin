package com.setvect.bokslcoin.autotrading.backtest.vbsstop;

import com.google.gson.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.VbsStopService;
import com.setvect.bokslcoin.autotrading.exchange.service.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.service.OrderService;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.service.CandleService;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
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


        // === 2. 백테스팅 ===
        File dataDir = new File("./craw-data/minute");
        CandleDataIterator candleDataIterator = new CandleDataIterator(dataDir, condition);


        when(candleService.getMinute(anyInt(), anyString())).then((aa) -> {
            if (candleDataIterator.hasNext()) {
                return candleDataIterator.next();
            }
            return null;
        });


        vbsStopService.apply();
        TimeUnit.SECONDS.sleep(20);
        System.out.println("끝");
    }

    class CandleDataIterator implements Iterator<CandleMinute> {
        private final File dataDir;
        private final VbsStopCondition condition;
        private Iterator<CandleMinute> currentCandleIterator;
        List<CandleMinute> currentCandleBundle;
        LocalDateTime current;

        public CandleDataIterator(File dataDir, VbsStopCondition condition) throws IOException {
            this.dataDir = dataDir;
            this.condition = condition;
            current = condition.getRange().getFrom();
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
                log.debug(next.toString());
                return next;
            }
            throw new NoSuchElementException();
        }

        @SneakyThrows
        private List<CandleMinute> nextBundle() {
            String dataFileName = String.format("%s-minute(%s).json", condition.getMarket(), DateUtil.format(current, "yyyy-MM"));
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
            current = current.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).plusMonths(1);
            return candleFiltered;
        }
    }
}
