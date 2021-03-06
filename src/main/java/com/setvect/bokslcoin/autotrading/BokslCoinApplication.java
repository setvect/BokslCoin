package com.setvect.bokslcoin.autotrading;

import com.setvect.bokslcoin.autotrading.starter.TradingWebsocket;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
public class BokslCoinApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(BokslCoinApplication.class, args);
        TradingWebsocket tradingWebsocket = context.getBean(TradingWebsocket.class);
        tradingWebsocket.onApplicationEvent();
    }
}
