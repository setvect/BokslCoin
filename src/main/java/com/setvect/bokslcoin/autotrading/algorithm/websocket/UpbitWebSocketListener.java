package com.setvect.bokslcoin.autotrading.algorithm.websocket;

import com.google.gson.annotations.SerializedName;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.starter.TradingWebsocket;
import com.setvect.bokslcoin.autotrading.util.BeanUtils;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class UpbitWebSocketListener extends WebSocketListener {
    private final ApplicationEventPublisher publisher;
    private final SlackMessageService slackMessageService;

    public UpbitWebSocketListener(ApplicationEventPublisher publisher, SlackMessageService slackMessageService) {
        super();
        this.publisher = publisher;
        this.slackMessageService = slackMessageService;
    }

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
        String message = String.format("Socket Closed : %s / %s", code, reason);
        log.info(message);
        slack(message);
    }


    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        String message = String.format("Socket Closing : %s / %s\n", code, reason);
        log.info(message);
        slack(message);
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
        webSocket.cancel();
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        String message = "Socket Error : " + t.getMessage();
        log.error(message, t);
        slack(message);

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }

        log.info("restarting");
        TradingWebsocket tradingWebsocket = BeanUtils.getBean(TradingWebsocket.class);
        tradingWebsocket.onApplicationEvent();
        log.info("restart completed");
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        TradeResult tradeResult = GsonUtil.GSON.fromJson(bytes.string(StandardCharsets.UTF_8), TradeResult.class);
        publisher.publishEvent(new ChangeTrade(tradeResult));
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

    private void slack(String message) {
        if (slackMessageService == null) {
            return;
        }
        slackMessageService.sendMessage(message);
    }


}
