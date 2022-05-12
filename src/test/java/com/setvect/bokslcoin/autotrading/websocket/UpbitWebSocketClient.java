package com.setvect.bokslcoin.autotrading.websocket;

import com.setvect.bokslcoin.autotrading.algorithm.websocket.UpbitWebSocketListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.Arrays;

public class UpbitWebSocketClient {
    public static void main(String[] args) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("wss://api.upbit.com/websocket/v1")
                .build();

        UpbitWebSocketListener webSocketListener = new UpbitWebSocketListener(System.out::println, null);
        webSocketListener.setParameter(Arrays.asList("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-EOS", "KRW-ETC", "KRW-ADA", "KRW-MANA", "KRW-BAT", "KRW-BCH", "KRW-DOT"));
//        webSocketListener.setParameter(Arrays.asList("KRW-XRP"));

        client.newWebSocket(request, webSocketListener);
        client.dispatcher().executorService().shutdown();
    }
}
