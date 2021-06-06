package com.setvect.bokslcoin.autotrading.schedule.service;

import com.setvect.bokslcoin.autotrading.exchange.service.AccountApiService;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.service.CandleApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TradingScheduleService {
    private final AccountApiService accountApiService;
    private final CandleApiService candleApiService;

    @Scheduled(fixedDelay = 5000, initialDelay = 1000)
    public void scheduleCheck() {
        List<Account> account = accountApiService.call();
        System.out.println(account);

        List<CandleMinute> candleList = candleApiService.callMinute(1, "KRW-BTC", LocalDateTime.now(), 10);
        System.out.println(candleList);
    }
}
