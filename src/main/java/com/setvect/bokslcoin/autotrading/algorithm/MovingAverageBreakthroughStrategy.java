package com.setvect.bokslcoin.autotrading.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 이동평균선 돌파 전략
 */
@Service("nabs")
@Slf4j
public class MovingAverageBreakthroughStrategy implements CoinTrading {
    @Override
    public void apply() {
        log.info("call MovingAverageBreakthroughStrategy");
    }
}
