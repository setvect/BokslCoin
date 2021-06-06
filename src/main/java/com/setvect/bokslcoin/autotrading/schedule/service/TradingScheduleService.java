package com.setvect.bokslcoin.autotrading.schedule.service;

import com.setvect.bokslcoin.autotrading.exchange.service.AccountApiService;
import com.setvect.bokslcoin.autotrading.model.Ticker;
import com.setvect.bokslcoin.autotrading.quotation.service.CandleApiService;
import com.setvect.bokslcoin.autotrading.quotation.service.TickerApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingScheduleService {
    private final AccountApiService accountApiService;
    private final CandleApiService candleApiService;
    private final TickerApiService tickerApiService;

    @Scheduled(fixedDelay = 5000, initialDelay = 1000)
    public void scheduleCheck() {
        log.info("call scheduleCheck()");
//        List<Account> account = accountApiService.call();
//        System.out.println(account);
//        List<CandleMinute> candleList = candleApiService.callMinute(1, "KRW-BTC", LocalDateTime.now(), 10);
//        System.out.println(candleList);
        List<Ticker> tickerList = tickerApiService.callTicker("KRW-BTC");
        System.out.println(tickerList);
    }
}
