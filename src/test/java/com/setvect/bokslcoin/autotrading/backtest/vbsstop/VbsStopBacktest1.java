package com.setvect.bokslcoin.autotrading.backtest.vbsstop;

import com.google.gson.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 변동성 돌파 전략 + 손절, 익절 알고리즘 백테스트
 */
public class VbsStopBacktest1 {
    @Test
    public void backtest() throws IOException {
        // === 1. 변수값 설정 ===
        VbsStopCondition condition = VbsStopCondition.builder()
                .k(0.5) // 변동성 돌파 판단 비율
                .rate(0.5) // 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
                .range(new DateRange("2017-09-20T00:00:00", "2021-06-08T23:59:59"))// 분석 대상 기간
                .coin("KRW-BTC")// 대상 코인
                .cash(10_000_000) // 최초 투자 금액
                .tradeMargin(1_000)// 매매시 채결 가격 차이
                .feeBid(0.0005) //  매수 수수료
                .feeAsk(0.0005)//  매도 수수료
                .loseRate(0.05) // 손절 라인
                .gainRate(0.1) //익절 라인
                .build();

        // === 2. 백테스팅 ===
        File dataDir = new File("./craw-data/minute");
        DateRange range = condition.getRange();
        LocalDateTime current = range.getFrom();
        double maxDiff = 0;
        CandleMinute maxCandle = null;
        while (condition.getRange().isBetween(current)) {
            String dataFileName = String.format("%s-minute(%s).json", condition.getCoin(), DateUtil.format(current, "yyyy-MM"));
            File dataFile = new File(dataDir, dataFileName);
            List<CandleMinute> candles = GsonUtil.GSON.fromJson(FileUtils.readFileToString(dataFile, "utf-8"), new TypeToken<List<CandleMinute>>() {
            }.getType());
            System.out.printf("load data file: %s%n", dataFileName);

            // 과거 데이터를 먼저(날짜 기준 오름 차순 정렬)
            Collections.reverse(candles);
            for (CandleMinute candle : candles) {
                if (!range.isBetween(candle.getCandleDateTimeUtc())) {
                    continue;
                }
                double diff = (candle.getHighPrice() / candle.getLowPrice() - 1) * 100;
                if (diff > maxDiff) {
                    maxDiff = diff;
                    maxCandle = candle;
                }
//                System.out.printf("%s: %,.0f, %.2f%n", candle.getCandleDateTimeUtc(), candle.getTradePrice(), diff);
            }
            current = current.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).plusMonths(1);
        }
        System.out.printf("분단위 최대 낙폭: %.2f%%%n", maxDiff);
        System.out.printf("%s%n", maxCandle);

        // === 3. 리포트 ===
        System.out.println("끝.");
    }
}
