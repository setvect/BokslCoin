package com.setvect.bokslcoin.autotrading.algorithm.common;

import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 매매 공통 모듈
 * static한 메소드 모음
 */
public class TradeCommonUtil {
    /**
     * 최소 매매 금액
     */
    public static final int MINIMUM_BUY_CASH = 5_000;

    private TradeCommonUtil() {
        // not instance
    }

    @Nullable
    public static String removeKrw(String market) {
        return StringUtils.replace(market, "KRW-", "");
    }

    /**
     * @param nowUtc     기준 시간
     * @param periodType 사용하는 매매 주기
     * @return 현재 매매 주기
     */
    public static int getCurrentPeriod(LocalDateTime nowUtc, PeriodType periodType) {
        int dayHourMinuteSum = nowUtc.getDayOfMonth() * 1440 + nowUtc.getHour() * 60 + nowUtc.getMinute();
        return dayHourMinuteSum / periodType.getDiffMinutes();
    }

    public static double getYield(Candle candle, Account account) {
        double avgPrice = account.getAvgBuyPriceValue();
        double diff = candle.getTradePrice() - avgPrice;
        return diff / avgPrice;
    }

}
