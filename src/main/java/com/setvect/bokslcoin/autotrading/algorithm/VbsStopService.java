package com.setvect.bokslcoin.autotrading.algorithm;

import com.setvect.bokslcoin.autotrading.exchange.service.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.service.OrderService;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.service.CandleService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 변동성 돌파 전략 + 손절, 익절 알고리즘
 */
@Service("vbsStop")
@Slf4j
@RequiredArgsConstructor
public class VbsStopService implements CoinTrading {
    private final AccountService accountService;
    private final CandleService candleService;
    private final OrderService orderService;
    private final TradeEvent tradeEvent;

    /**
     * 매수, 매도 대상 코인
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbsStop.market}")
    private String market;

    /**
     * 변동성 돌파 판단 비율
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbsStop.k}")
    private double k;

    /**
     * 총 현금을 기준으로 투자 비율
     * 1은 100%, 0.5은 50% 투자
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbsStop.investRatio}")
    private double investRatio;

    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbsStop.loseStopRate}")
    private double loseStopRate;

    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbsStop.gainStopRate}")
    private double gainStopRate;

    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbsStop.tradePeriod}")
    private TradePeriod tradePeriod;

    /**
     * 매수 접수 시간 범위
     */
    private DateRange bidRange;
    /**
     * 매도 접수 시간 범위
     */
    private DateRange askRange;
    /**
     * 해당 기간에 매매 여부 완료 여부
     */
    private boolean tradeCompleteOfPeriod;

    private int day = 0;
    /**
     * 매수 목표 주가, 해당 가격 이상이면 매수
     */
    private Double targetPrice;

    /**
     * 매도 유형
     */
    public enum AskReason {
        /**
         * 매도 시간 경과
         */
        TIME,
        /**
         * 손절 매도
         */
        LOSS,
        /**
         * 익절 매도
         */
        GAIN
    }

    /**
     * 매매 주기
     */
    @AllArgsConstructor
    @Getter
    public enum TradePeriod {
        P_60(55, 4, 1),
        P_240(230, 9, 1),
        P_1440(1410, 29, 1);
        /**
         * 매수 기간(분)
         */
        int bidMinute;
        /**
         * 매수, 매도 사이에 매매가 일어나지 않는 시간(분)
         */
        int intermissionMinute;
        /**
         * 매도 기간(분)
         */
        int askMinute;

        public int getTotal() {
            return bidMinute + intermissionMinute + askMinute;
        }
    }

    @Override
    public void apply() {
        CandleMinute candle = candleService.getMinute(1, market);
        initTime(candle.getCandleDateTimeUtc(), tradePeriod);
        Optional<Account> coinAccount = accountService.getAccount(market);
        BigDecimal coinBalance = AccountService.getBalance(coinAccount);
        double currentPrice = candle.getTradePrice();

        //  코인 매수를 했다면
        LocalDateTime now = candle.getCandleDateTimeUtc();

        // 새로운 날짜면 매매 다시 초기화
        if (day != now.getDayOfMonth()) {
            log.info("change Date: " + candle.getCandleDateTimeKst());
            tradeCompleteOfPeriod = false;
            day = now.getDayOfMonth();

            targetPrice = getTargetPrice();
            if (targetPrice == null) {
                return;
            }

            tradeEvent.registerTargetPrice(targetPrice);
        }
        if (targetPrice == null) {
            return;
        }

        log.debug(String.format("현재 시간: %s, 매수 시간: %s, 매도 시간: %s, %s: %,f", DateUtil.formatDateTime(LocalDateTime.now()), bidRange, askRange, market, currentPrice));

        double balance = coinBalance.doubleValue();
        if (balance > 0.00001) {
            double rate = getYield(candle, coinAccount);
            Account account = coinAccount.get();
            log.info(String.format("매입단가: %,.0f, 현재가격: %,.0f, 투자금: %,.0f, 수익율: %.2f%%",
                    Double.valueOf(account.getAvgBuyPrice()),
                    candle.getTradePrice(),
                    getInvestCash(account),
                    rate * 100));
            // 매도 시간 파악
            AskReason reason = null;
            if (askRange.isBetween(now)) {
                reason = AskReason.TIME;
            }
            // 이익인 경우
            else if (rate > 0) {
                if (this.gainStopRate <= rate) {
                    reason = AskReason.GAIN;
                }
            }
            // 손실인 경우
            else {
                if (this.loseStopRate <= -rate) {
                    reason = AskReason.LOSS;
                }
            }

            if (reason != null) {
                doAsk(currentPrice, balance, reason);
            }

        } else if (bidRange.isBetween(now) && !tradeCompleteOfPeriod) {
//            log.debug(String.format("%s 목표가: %,f\t현재가: %,f", market, targetValue, currentPrice));

            if (targetPrice > currentPrice) {
//                log.debug("목표가 도달하지 않음");
                return;
            }
            doBid(currentPrice);
        }
    }

    /**
     * @param coinAccount
     * @return 원화 기준 투자금
     */
    private double getInvestCash(Account coinAccount) {
        return Double.valueOf(coinAccount.getAvgBuyPrice()) * Double.valueOf(coinAccount.getBalance());
    }

    private void doBid(double currentPrice) {
        BigDecimal krw = accountService.getBalance("KRW");
        // 매수 금액
        double bidPrice = krw.doubleValue() * investRatio;
        orderService.callOrderBidByMarket(market, ApplicationUtil.toNumberString(bidPrice));
        tradeEvent.bid(market, currentPrice, bidPrice);
    }

    private void doAsk(double currentPrice, double balance, AskReason reason) {
        orderService.callOrderAskByMarket(market, ApplicationUtil.toNumberString(balance));
        tradeEvent.ask(market, balance, currentPrice, reason);
        tradeCompleteOfPeriod = true;
    }

    private double getYield(CandleMinute candle, Optional<Account> account) {
        double avgPrice = AccountService.getAvgBuyPrice(account).get().doubleValue();
        double diff = candle.getTradePrice() - avgPrice;
        return diff / avgPrice;
    }

    /**
     * 매수, 매도 시간 범위 설정
     * <p>
     * bidMinute + intermissionMinute + askMinute 합이 1440분(하루)의 약수로 입력
     * 예)
     * 55 + 4 + 1 = 60
     * 230 + 9 + 1 = 240
     * 1410 + 29 + 1 = 1440
     *
     * @param baseDate    기준 날짜
     * @param tradePeriod 매매 주기
     */
    private void initTime(LocalDateTime baseDate, TradePeriod tradePeriod) {
        int total = tradePeriod.getTotal();
        int minuteOfDay = baseDate.getHour() * 60 + baseDate.getMinute();

        int hour = (minuteOfDay / total) * (total / 60);
        LocalTime bidFrom = LocalTime.of(hour, 0);
        LocalTime bidTo = bidFrom.plusMinutes(tradePeriod.getBidMinute());

        LocalTime askFrom = bidTo.plusMinutes(tradePeriod.getIntermissionMinute());
        LocalTime askTo = askFrom.plusMinutes(tradePeriod.getAskMinute());

        // 매도 범위
        this.bidRange = ApplicationUtil.getDateRange(baseDate.toLocalDate(), bidFrom, bidTo);
        this.askRange = ApplicationUtil.getDateRange(baseDate.toLocalDate(), askFrom, askTo);
    }

    /**
     * @return 매수를 하기위한 목표 가격
     */
    private Double getTargetPrice() {
        Candle after = getBeforePeriod();
        if (after == null) {
            return null;
        }

        double targetPrice = after.getTradePrice() + (after.getHighPrice() - after.getLowPrice()) * k;
        log.debug(String.format("목표가: %,.2f = 종가: %,.2f + (고가: %,.2f - 저가: %,.2f) * K값: %,.2f"
                , targetPrice, after.getTradePrice(), after.getHighPrice(), after.getLowPrice(), k));

        return targetPrice;
    }

    private Candle getBeforePeriod() {
        switch (tradePeriod) {
            case P_60:
                List<CandleMinute> t1 = candleService.getMinute(60, market, 2);
                return t1.get(1);
            case P_240:
                List<CandleMinute> t2 = candleService.getMinute(240, market, 2);
                return t2.get(1);
            case P_1440:
                List<CandleDay> t3 = candleService.getDay(market, 2);
                return t3.get(1);
        }
        return null;
    }
}
