package com.setvect.bokslcoin.autotrading.algorithm.websocket;

import com.google.gson.annotations.SerializedName;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class UpbitWebSocketListener extends WebSocketListener {
    @Getter
    public enum UpbitType {
        /**
         * 현재가
         */
        @SerializedName("ticker")
        TICKER,
        /**
         * 체결
         */
        @SerializedName("trade")
        TRADE,
        /**
         * 호가
         */
        @SerializedName("orderbook")
        ORDERBOOK
    }

    private static final int NORMAL_CLOSURE_STATUS = 1000;
    /**
     * TRADE 타입으로 고정
     */
    private static final UpbitType TYPE_TRADE = UpbitType.TRADE;
    private String json;

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        System.out.printf("Socket Closed : %s / %s\r\n", code, reason);
    }

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        System.out.printf("Socket Closing : %s / %s\n", code, reason);
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
        webSocket.cancel();
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        System.out.println("Socket Error : " + t.getMessage());
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        TradeResult tradeResult = GsonUtil.GSON.fromJson(bytes.string(StandardCharsets.UTF_8), TradeResult.class);
        System.out.println(tradeResult);
        System.out.println(tradeResult.getTradeDateTimeKst());
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        webSocket.send(getParameter());
    }

    public void setParameter(List<String> codes) {
        this.json = GsonUtil.GSON.toJson(Arrays.asList(Ticket.of(UUID.randomUUID().toString()), Type.of(TYPE_TRADE, codes)));
    }

    private String getParameter() {
        return this.json;
    }

    @Getter
    @RequiredArgsConstructor(staticName = "of")
    private static class Ticket {
        private final String ticket;
    }

    @Getter
    @RequiredArgsConstructor(staticName = "of")
    private static class Type {
        private final UpbitType type;
        private final List<String> codes;
    }
}
