package com.setvect.bokslcoin.autotrading.backtest.common;

import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;

import java.util.Map;

public class BacktestHelper {
    private BacktestHelper() {
        // not instance
    }

    public static Candle depthCopy(Candle candle) {
        String json = GsonUtil.GSON.toJson(candle);
        return GsonUtil.GSON.fromJson(json, Candle.class);
    }

    /**
     * @param accountMap 코인(현금 포함) 계좌
     * @return 현재 투자한 코인 함
     */
    public static double getBuyTotalAmount(Map<String, Account> accountMap) {
        return accountMap.entrySet().stream().filter(e -> !e.getKey().equals("KRW")).mapToDouble(e -> e.getValue().getInvestCash()).sum();
    }

}
