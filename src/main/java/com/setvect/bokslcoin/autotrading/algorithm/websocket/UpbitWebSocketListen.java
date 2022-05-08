package com.setvect.bokslcoin.autotrading.algorithm.websocket;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocketListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UpbitWebSocketListen {
    @Value("${com.setvect.bokslcoin.autotrading.ws.url}")
    private String url;

    public void listen(WebSocketListener listener) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();

    }
}
