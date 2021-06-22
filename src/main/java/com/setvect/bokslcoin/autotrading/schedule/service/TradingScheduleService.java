package com.setvect.bokslcoin.autotrading.schedule.service;

import com.setvect.bokslcoin.autotrading.algorithm.CoinTrading;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TradingScheduleService {
    private final CoinTrading conCoinTrading;
    private final SlackMessageService slackMessageService;

    private final boolean enable;

    public TradingScheduleService(
            ApplicationContext context,
            @Value("${com.setvect.bokslcoin.autotrading.algorithm.name}") String name, @Value("${com.setvect.bokslcoin.autotrading.enable}") String enable) {
        this.enable = Boolean.parseBoolean(enable);
        this.conCoinTrading = (CoinTrading) context.getBean(name);
        this.slackMessageService = context.getBean(SlackMessageService.class);
    }

    @Scheduled(fixedRateString = "${com.setvect.bokslcoin.autotrading.schedule.fixedDelay}", initialDelay = 1000)
    public void scheduleCheck() {
        if (!enable) {
            log.debug("skip");
            return;
        }
        try {
            conCoinTrading.apply();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            slackMessageService.sendMessage(e.getMessage());
        }
    }
}
