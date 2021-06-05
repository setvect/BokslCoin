package com.setvect.bokslcoin.autotrading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UpbitAutoTradingApplication {
    public static void main(String[] args) {
        SpringApplication.run(UpbitAutoTradingApplication.class, args);
    }
}
