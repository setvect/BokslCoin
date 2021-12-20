package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class MabsTradeAnalyzer {
    @Test
    public void singleBacktest(){
        System.out.println("끝");
    }
}
