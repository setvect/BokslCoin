package com.setvect.bokslcoin.autotrading.schedule.service;

import com.setvect.bokslcoin.autotrading.algorithm.CoinTrading;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TradingScheduleService {
    private final CoinTrading conCoinTrading;

    public TradingScheduleService(
            ApplicationContext context,
            @Value("${com.setvect.bokslcoin.autotrading.algorithm.name}") String name) {
        this.conCoinTrading = (CoinTrading) context.getBean(name);
    }

    @Scheduled(fixedRateString = "${com.setvect.bokslcoin.autotrading.schedule.fixedDelay}", initialDelay = 1000)
    public void scheduleCheck() {
        conCoinTrading.apply();
    }
}
