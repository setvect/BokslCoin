package com.setvect.bokslcoin.autotrading.starter;

import com.setvect.bokslcoin.autotrading.algorithm.websocket.UpbitWebSocketListen;
import com.setvect.bokslcoin.autotrading.algorithm.websocket.UpbitWebSocketListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
@RequiredArgsConstructor
public class TradingWebsocket {
    private final UpbitWebSocketListen upbitWebSocketListen;
    private final ApplicationEventPublisher publisher;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        UpbitWebSocketListener webSocketListener = new UpbitWebSocketListener(publisher);
        webSocketListener.setParameter(Arrays.asList("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-EOS", "KRW-ETC", "KRW-ADA", "KRW-MANA", "KRW-BAT", "KRW-BCH", "KRW-DOT"));

        upbitWebSocketListen.listen(webSocketListener);
    }
}