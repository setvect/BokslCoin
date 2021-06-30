package com.setvect.bokslcoin.autotrading.algorithm.vbs;

import com.setvect.bokslcoin.autotrading.algorithm.AskReason;
import com.setvect.bokslcoin.autotrading.algorithm.CoinTrading;
import com.setvect.bokslcoin.autotrading.algorithm.TradePeriod;
import com.setvect.bokslcoin.autotrading.exchange.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.OrderService;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 변동성 돌파 + 손절 + 트레일링 스탑 알고리즘
 */
@Service("vbsTrailingStop")
@Slf4j
@RequiredArgsConstructor
public class VbsTrailingStopService implements CoinTrading {
    private final AccountService accountService;
    private final CandleService candleService;
    private final OrderService orderService;
    private final TradeEvent tradeEvent;

    /**
     * 매수, 매도 대상 코인
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbsTrailingStop.market}")
    private String market;

    /**
     * 변동성 돌파 판단 비율
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbsTrailingStop.k}")
    private double k;

    /**
     * 총 현금을 기준으로 투자 비율
     * 1은 100%, 0.5은 50% 투자
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbsTrailingStop.investRatio}")
    private double investRatio;

    /**
     * 손절 매도
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbsTrailingStop.loseStopRate}")
    private double loseStopRate;

    /**
     * 트레일링 스탑 진입점
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbsTrailingStop.gainStopRate}")
    private double gainStopRate;

    /**
     * gainStopRate 이상 상승 후 전고점 대비 trailingStopRate 비율 만큼 하락하면 시장가 매도
     * 예를 들어 trailingStopRate 값이 0.02일 때 고점 수익률이 12%인 상태에서 10%미만으로 떨어지면 시장가 매도
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbsTrailingStop.trailingStopRate}")
    private double trailingStopRate;

    /**
     * 매매 주기
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbsTrailingStop.tradePeriod}")
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

    private int periodIdx = -1;
    /**
     * 매수 목표 주가, 해당 가격 이상이면 매수
     */
    private Double targetPrice;
    /**
     * 트레일링 진입점 돌파 여부
     */
    @Getter
    private boolean trailingTrigger = false;
    /**
     * 매수 이후 고점 수익률
     */
    @Getter
    private double highYield = 0;

    @Override
    public void apply() {
        CandleMinute candle = candleService.getMinute(1, market);
        ZonedDateTime nowUtcZoned = candle.getCandleDateTimeUtc().atZone(ZoneId.of("UTC"));
        LocalDateTime nowUtc = nowUtcZoned.toLocalDateTime();
        LocalDateTime nowKst = candle.getCandleDateTimeKst();
        initTime(nowUtc, tradePeriod);
        Optional<Account> coinAccount = accountService.getAccount(market);
        BigDecimal coinBalance = AccountService.getBalance(coinAccount);
        double currentPrice = candle.getTradePrice();

        int dayHourMinuteSum = nowUtc.getDayOfMonth() * 1440 + nowUtc.getHour() * 60 + nowUtc.getMinute();
        int currentPeriod = dayHourMinuteSum / tradePeriod.getTotal();

        // 새로운 날짜면 매매 다시 초기화
        if (periodIdx != currentPeriod) {
            tradeEvent.newPeriod(candle);
            tradeCompleteOfPeriod = false;
            periodIdx = currentPeriod;

            targetPrice = getTargetPrice();
            if (targetPrice == null) {
                return;
            }
            tradeEvent.registerTargetPrice(market, targetPrice);
        }
        tradeEvent.check(candle);
        if (targetPrice == null) {
            return;
        }

        log.debug(String.format("현재 시간: %s, 매수 시간: %s, 매도 시간: %s, %s: %,.0f", DateUtil.formatDateTime(LocalDateTime.now()), bidRange, askRange, market, currentPrice));

        double balance = coinBalance.doubleValue();
        // 코인을 매수 했다면 매도 조건 판단
        if (balance > 0.00001) {
            double rate = getYield(candle, coinAccount);
            highYield = Math.max(highYield, rate);

            Account account = coinAccount.get();
            log.debug(String.format("매입단가: %,.0f, 현재가격: %,.0f, 투자금: %,.0f, 수익율: %.2f%%, 트레일링 스탑: %s, 최고 수익률: %.2f%%,",
                    Double.valueOf(account.getAvgBuyPrice()),
                    candle.getTradePrice(),
                    getInvestCash(account),
                    rate * 100,
                    trailingTrigger,
                    highYield * 100
            ));

            // 매도 시간 파악
            AskReason reason = null;
            // 손절 판단
            if (-this.loseStopRate >= rate) {
                reason = AskReason.LOSS;
            } else if (trailingTrigger) {
                // 트레일링 스탑 하락 검증 판단
                if (rate < highYield - trailingStopRate) {
                    reason = AskReason.GAIN;
                }
            }
            // 트레링이 스탑 진입 판단
            else if (this.gainStopRate <= rate) {
                trailingTrigger = true;
            }
            //  매도 시간
            else if (askRange.isBetween(nowKst)) {
                reason = AskReason.TIME;
            }

            if (reason != null) {
                doAsk(currentPrice, balance, reason);
            }

        } else if (bidRange.isBetween(nowKst) && !tradeCompleteOfPeriod) {
            log.debug(String.format("%s 목표가: %,.0f\t현재가: %,.0f", market, targetPrice, currentPrice));

            if (targetPrice > currentPrice) {
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
        trailingTrigger = false;
        highYield = 0;
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
