package com.setvect.bokslcoin.autotrading.starter;

import com.setvect.bokslcoin.autotrading.algorithm.mabs.MabsMultiProperties;
import com.setvect.bokslcoin.autotrading.algorithm.websocket.UpbitWebSocketListen;
import com.setvect.bokslcoin.autotrading.algorithm.websocket.UpbitWebSocketListener;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 실시간으로 시세를 확인 하여 매매 진행
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TradingWebsocket {
    private final UpbitWebSocketListen upbitWebSocketListen;
    private final ApplicationEventPublisher publisher;
    private final SlackMessageService slackMessageService;
    private final MabsMultiProperties properties;


    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        UpbitWebSocketListener webSocketListener = new UpbitWebSocketListener(publisher, slackMessageService);
        webSocketListener.setParameter(properties.getMarkets());

        upbitWebSocketListen.listen(webSocketListener);
    }
}