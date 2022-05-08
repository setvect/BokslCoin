package com.setvect.bokslcoin.autotrading.algorithm.websocket;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ChangeTrade extends ApplicationEvent {

    private final TradeResult tradeResult;

    public ChangeTrade(TradeResult tradeResult) {
        super(tradeResult);
        this.tradeResult = tradeResult;
    }
}
