package com.setvect.bokslcoin.autotrading.algorithm.websocket;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

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
