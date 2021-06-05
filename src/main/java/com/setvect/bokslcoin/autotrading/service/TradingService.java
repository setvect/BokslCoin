package com.setvect.bokslcoin.autotrading.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TradingService {

    @Value("${com.setvect.bokslcoin.autotrading.accessKey}")
    private String accessKey;

    @Value("${com.setvect.bokslcoin.autotrading.secretKey}")
    private String secretKey;

    @Scheduled(fixedDelay = 5000, initialDelay = 1000)
    public void scheduleFixedDelayTask() {
        System.out.println("Fixed delay task - " + System.currentTimeMillis() / 1000);
        System.out.println(accessKey + "\t" + secretKey);
    }
}
