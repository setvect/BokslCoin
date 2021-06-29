package com.setvect.bokslcoin.autotrading.api;

import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class CandleServiceTest {
    @Autowired
    private CandleService candleService;

    @Test
    public void candleTest() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            List<CandleMinute> candleList = candleService.getMinute(1, "KRW-BTC", 2, LocalDateTime.now());
            Candle candleDay = candleList.get(0);
            System.out.println(candleDay.getCandleDateTimeKst() + ": " + ApplicationUtil.toNumberString(candleDay.getTradePrice()));
            TimeUnit.SECONDS.sleep(1);
        }

        for (int i = 0; i < 20; i++) {
            List<CandleDay> candleList = candleService.getDay("KRW-BTC", 1, LocalDateTime.now());
            Candle candleDay = candleList.get(0);
            System.out.println(candleDay.getCandleDateTimeKst() + ": " + ApplicationUtil.toNumberString(candleDay.getTradePrice()));
            TimeUnit.SECONDS.sleep(1);
        }
        System.out.println("끝.");
    }
}
