package com.setvect.bokslcoin.autotrading.algorithm.websocket;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class UpbitWebSocketListen {
    @Value("${com.setvect.bokslcoin.autotrading.ws.url}")
    private String url;

    public void listen() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        UpbitWebSocketListener webSocketListener = new UpbitWebSocketListener();
        webSocketListener.setParameter(Arrays.asList("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-EOS", "KRW-ETC", "KRW-ADA", "KRW-MANA", "KRW-BAT", "KRW-BCH", "KRW-DOT"));
//        webSocketListener.setParameter(Arrays.asList("KRW-XRP"));

        client.newWebSocket(request, webSocketListener);
        client.dispatcher().executorService().shutdown();

    }
}
