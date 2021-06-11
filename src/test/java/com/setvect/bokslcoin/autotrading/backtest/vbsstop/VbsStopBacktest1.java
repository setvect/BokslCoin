package com.setvect.bokslcoin.autotrading.backtest.vbsstop;

import com.google.gson.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.algorithm.TradeService;
import com.setvect.bokslcoin.autotrading.algorithm.VbsStopServiceDeleteMe;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 변동성 돌파 전략 + 손절, 익절 알고리즘 백테스트
 */
public class VbsStopBacktest1 {
    @Test
    public void backtest() throws IOException {
        // === 1. 변수값 설정 ===
        VbsStopCondition condition = VbsStopCondition.builder()
                .k(0.5) // 변동성 돌파 판단 비율
                .investRatio(0.5) // 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
                .range(new DateRange("2021-01-01T00:00:00", "2021-01-07T23:59:59"))// 분석 대상 기간
                .market("KRW-BTC")// 대상 코인
                .cash(10_000_000) // 최초 투자 금액
                .tradeMargin(1_000)// 매매시 채결 가격 차이
                .feeBid(0.0005) //  매수 수수료
                .feeAsk(0.0005)//  매도 수수료
                .loseRate(0.05) // 손절 라인
                .gainRate(0.1) //익절 라인
                .build();

        TestTradeService tradeService = new TestTradeService(condition);

        VbsStopServiceDeleteMe vbsStopService = VbsStopServiceDeleteMe.builder()
                .tradeService(tradeService)
                .market(condition.getMarket())
                .k(condition.getK())
                .investRatio(condition.getInvestRatio())
                .gainStop(condition.getGainRate())
                .loseStop(condition.getLoseRate())
                .build();

        // === 2. 백테스팅 ===
        File dataDir = new File("./craw-data/minute");
        DateRange range = condition.getRange();
        LocalDateTime current = range.getFrom();
        Candle beforePeriod = null;
        Candle currentPeriod = null;

        while (condition.getRange().isBetween(current)) {
            String dataFileName = String.format("%s-minute(%s).json", condition.getMarket(), DateUtil.format(current, "yyyy-MM"));
            File dataFile = new File(dataDir, dataFileName);
            List<CandleMinute> candles = GsonUtil.GSON.fromJson(FileUtils.readFileToString(dataFile, "utf-8"), new TypeToken<List<CandleMinute>>() {
            }.getType());
            System.out.printf("load data file: %s%n", dataFileName);

            // 과거 데이터를 먼저(날짜 기준 오름 차순 정렬)
            Collections.reverse(candles);
            int currentDay = 0;
            for (CandleMinute candle : candles) {
                if (!range.isBetween(candle.getCandleDateTimeUtc())) {
                    continue;
                }
                int day = candle.getCandleDateTimeUtc().getDayOfMonth();
                if (currentDay != day) {
                    tradeService.setBuyPrice(currentDay);
                    tradeService.setCurrentPeriod(currentPeriod);
                    if (beforePeriod != null) {
                        tradeService.setBeforeTradePrice(beforePeriod.getTradePrice());
                    }
                    tradeService.endPeriod();

                    currentDay = day;
                    // todo
                    beforePeriod = currentPeriod;
                    currentPeriod = new Candle();
                    currentPeriod.setCandleDateTimeUtc(candle.getCandleDateTimeUtc());

                    currentPeriod.setCandleDateTimeKst(candle.getCandleDateTimeKst());
                    currentPeriod.setOpeningPrice(candle.getOpeningPrice());
                }

                currentPeriod.setHighPrice(Math.max(currentPeriod.getHighPrice(), candle.getHighPrice()));
                currentPeriod.setLowPrice(Math.min(currentPeriod.getLowPrice() == 0 ? Integer.MAX_VALUE : currentPeriod.getLowPrice(), candle.getLowPrice()));
                currentPeriod.setTradePrice(candle.getTradePrice());
                Optional<Account> account = tradeService.getAccount();
                vbsStopService.process(candle, beforePeriod, account);

            }
            current = current.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).plusMonths(1);
        }

        // === 3. 리포트 ===
        System.out.println("끝.");
    }
}

class TestTradeService implements TradeService {
    private final VbsStopCondition condition;
    private Account account = null;
    private double buyPrice;
    private Candle currentPeriod;

    private List<VbsStopBacktestRow> tradeHistory = new ArrayList<>();
    private boolean isTrade;
    private double targetPrice;
    private double bidPrice;
    private double askPrice;
    private double investment;
    private double cash;
    private double beforeTradePrice;

    public void setBeforeTradePrice(double beforeTradePrice) {
        this.beforeTradePrice = beforeTradePrice;
    }

    public TestTradeService(VbsStopCondition condition) {
        this.condition = condition;
    }

    @Override
    public void bid(double investment, double cash) {
        account = new Account();
        account.setAvgBuyPrice(ApplicationUtil.toNumberString(buyPrice));
        account.setBalance(ApplicationUtil.toNumberString(condition.getCash()));
        this.bidPrice = targetPrice + condition.getTradeMargin();
        this.cash = cash;
        this.investment = investment;
        account.setAvgBuyPrice(ApplicationUtil.toNumberString(investment));
        account.setBalance(ApplicationUtil.toNumberString(bidPrice));
        System.out.printf("매수목표가: %,f, 매수가: %,f, 현재가: %,f, 매수금액:%,f\n", targetPrice, bidPrice, buyPrice, investment);
    }

    @Override
    public void ask(Double askPrice, VbsStopServiceDeleteMe.AskType askType) {
        this.isTrade = true;
        this.askPrice = askPrice - condition.getTradeMargin();
    }

    @Override
    public void applyTargetPrice(double targetPrice) {
        this.targetPrice = targetPrice;
    }

    public void endPeriod() {
        if (currentPeriod == null) {
            return;
        }
        VbsStopBacktestRow trade = new VbsStopBacktestRow(currentPeriod);
        trade.setTargetPrice(targetPrice);
        trade.setTrade(isTrade);
        trade.setBidPrice(bidPrice);
        trade.setAskPrice(askPrice);
        trade.setInvestmentAmount(investment);
        trade.setCash(cash);
        trade.setBeforeTradePrice(beforeTradePrice);

        /**
         * 매수, 매도 수수료
         */
        double feeTotal = investment * condition.getFeeAsk() + investment * condition.getFeeBid();
        trade.setFeePrice(feeTotal);
        tradeHistory.add(trade);
        System.out.println(trade);
        isTrade = false;
        bidPrice = 0;
        askPrice = 0;
        account = null;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public void setCurrentPeriod(Candle currentPeriod) {
        this.currentPeriod = currentPeriod;
    }

    public Optional<Account> getAccount() {
        if (account == null) {
            return Optional.empty();
        }
        return Optional.of(account);
    }
}

