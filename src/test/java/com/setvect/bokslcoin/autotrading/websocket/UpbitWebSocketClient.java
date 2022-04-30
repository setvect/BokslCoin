package com.setvect.bokslcoin.autotrading.websocket;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.Arrays;

public class UpbitWebSocketClient {
    public static void main(String[] args) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("wss://api.upbit.com/websocket/v1")
                .build();

        UpbitWebSocketListener webSocketListener = new UpbitWebSocketListener();
//        webSocketListener.setParameter(UpbitType.TRADE, List.of("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-EOS", "KRW-ETC", "KRW-ADA", "KRW-MANA", "KRW-BAT", "KRW-BCH", "KRW-DOT"));
        webSocketListener.setParameter(UpbitType.TRADE, Arrays.asList("KRW-XRP"));

        client.newWebSocket(request, webSocketListener);
        client.dispatcher().executorService().shutdown();
    }
}
