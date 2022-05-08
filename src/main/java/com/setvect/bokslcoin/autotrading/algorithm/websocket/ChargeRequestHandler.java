package com.setvect.bokslcoin.autotrading.algorithm.websocket;

import com.setvect.bokslcoin.autotrading.algorithm.mabs.MabsMultiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ChargeRequestHandler implements ApplicationListener<ChangeTrade> {

    private final MabsMultiService mabsMultiService;

    @Override
    public void onApplicationEvent(ChangeTrade event) {
        mabsMultiService.tradeEvent(event.getTradeResult());
    }
}
