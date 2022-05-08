package com.setvect.bokslcoin.autotrading.algorithm.websocket;

import com.setvect.bokslcoin.autotrading.algorithm.CoinTrading;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChargeRequestHandler implements ApplicationListener<ChangeTrade> {

    private final CoinTrading conCoinTrading;

    public ChargeRequestHandler(ApplicationContext context, @Value("${com.setvect.bokslcoin.autotrading.algorithm.name}") String name) {
        this.conCoinTrading = (CoinTrading) context.getBean(name);
    }


    @Override
    public void onApplicationEvent(ChangeTrade event) {
        conCoinTrading.tradeEvent(event.getTradeResult());
    }
}
