package com.setvect.bokslcoin.autotrading.algorithm.mabs;

import com.setvect.bokslcoin.autotrading.algorithm.AskReason;
import com.setvect.bokslcoin.autotrading.algorithm.CoinTrading;
import com.setvect.bokslcoin.autotrading.algorithm.TradePeriod;
import com.setvect.bokslcoin.autotrading.algorithm.vbs.TradeEvent;
import com.setvect.bokslcoin.autotrading.exchange.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.OrderService;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * 이평선 돌파 전략
 */
@Service("mabs")
@Slf4j
@RequiredArgsConstructor
public class MabsService implements CoinTrading {
    private final AccountService accountService;
    private final CandleService candleService;
    private final OrderService orderService;
    private final TradeEvent tradeEvent;

    /**
     * 매수, 매도 대상 코인
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabs.market}")
    private String market;

    /**
     * 총 현금을 기준으로 투자 비율
     * 1은 100%, 0.5은 50% 투자
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabs.investRatio}")
    private double investRatio;

    /**
     * 매매 주기
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabs.tradePeriod}")
    private TradePeriod tradePeriod;

    /**
     * 상승 매수률
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabs.upBuyRate}")
    private double upBuyRate;

    /**
     * 하락 매도률
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabs.downSellRate}")
    private double downSellRate;

    /**
     * 단기 이동평균 기간
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabs.shortPeriod}")
    private int shortPeriod;

    /**
     * 장기 이동평균 기간
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabs.longPeriod}")
    private int longPeriod;

    /**
     * 해당 기간에 매매 여부 완료 여부
     */
    private boolean tradeCompleteOfPeriod;

    private int periodIdx = -1;

    /**
     * 매수 이후 고점 수익률
     */
    @Getter
    private double highYield = 0;

    @Override
    public void apply() {
        List<Candle> candleList = getCandleList(longPeriod);
        double maShort = getMa(candleList, shortPeriod);
        double maLong = getMa(candleList, longPeriod);

        Optional<Account> coinAccount = accountService.getAccount(market);
        BigDecimal coinBalance = AccountService.getBalance(coinAccount);

        CandleMinute candle = candleService.getMinute(1, market);
        double currentPrice = candle.getTradePrice();
        ZonedDateTime nowUtcZoned = candle.getCandleDateTimeUtc().atZone(ZoneId.of("UTC"));
        LocalDateTime nowUtc = nowUtcZoned.toLocalDateTime();
        LocalDateTime nowKst = candle.getCandleDateTimeKst();

        int currentPeriod = getCurrentPeriod(nowUtc);

        // 새로운 날짜면 매매 다시 초기화
        if (periodIdx != currentPeriod) {
            tradeEvent.newPeriod(candle);
            tradeCompleteOfPeriod = false;
            periodIdx = currentPeriod;
        }
        tradeEvent.check(candle);

        log.debug(String.format("KST:%s, UTC: %s, 현재가: %,.2f, MA_%d: %,.2f, MA_%d: %,.2f, 장기-단기 차이: %,.2f(%.2f%%)",
                DateUtil.formatDateTime(nowUtc),
                DateUtil.formatDateTime(nowKst),
                candle.getTradePrice(),
                shortPeriod, maShort,
                longPeriod, maLong,
                maLong - maShort, (maShort / maLong - 1) * 100));

        double balance = coinBalance.doubleValue();
        // 코인을 매수 했다면 매도 조건 판단
        if (balance > 0.00001) {
            double rate = getYield(candle, coinAccount);
            highYield = Math.max(highYield, rate);
            double sellTargetPrice = maShort + maShort * downSellRate;

            Account account = coinAccount.get();
            log.debug(String.format("매입단가: %,.2f, 투자금: %,.0f, 수익율: %.2f%%, 최고 수익률: %.2f%%, 매도목표가: %,.2f",
                    Double.valueOf(account.getAvgBuyPrice()),
                    getInvestCash(account),
                    rate * 100,
                    highYield * 100,
                    sellTargetPrice
            ));

            // 장기이평 >= (단기이평 + 단기이평 * 하락매도률)
            boolean isSell = maLong >= sellTargetPrice;
            log.debug(String.format("매도 조건: 장기이평 >= (단기이평 + 단기이평 * 하락매도률), %,.2f >= %,.2f ---> %s", maLong, sellTargetPrice, isSell));

            if (isSell) {
                doAsk(candle.getTradePrice(), balance, AskReason.MA_DOWN);
            }
        } else if (!tradeCompleteOfPeriod) {
            double buyTargetPrice = maLong + maLong * upBuyRate;

            //(장기이평 + 장기이평 * 상승매수률) <= 단기이평
            boolean isBuy = buyTargetPrice <= maShort;
            log.debug(String.format("매수 조건: (장기이평 + 장기이평 * 상승매수률) <= 단기이평, %,.2f <= %,.2f ---> %s", buyTargetPrice, maShort, isBuy));
            if (isBuy) {
                doBid(currentPrice);
            }
        }
    }

    private int getCurrentPeriod(LocalDateTime nowUtc) {
        int dayHourMinuteSum = nowUtc.getDayOfMonth() * 1440 + nowUtc.getHour() * 60 + nowUtc.getMinute();
        int currentPeriod = dayHourMinuteSum / tradePeriod.getTotal();
        return currentPeriod;
    }

    private double getMa(List<Candle> moveListCandle, int durationCount) {
        OptionalDouble val = moveListCandle.stream().limit(durationCount).mapToDouble(c -> c.getTradePrice()).average();
        return val.getAsDouble();
    }

    private List<Candle> getCandleList(int longPeriod) {
        List<Candle> moveListCandle = new ArrayList<>();
        switch (tradePeriod) {
            case P_60:
                List<CandleMinute> t1 = candleService.getMinute(60, market, longPeriod);
                moveListCandle.addAll(t1);
            case P_240:
                List<CandleMinute> t2 = candleService.getMinute(240, market, longPeriod);
                moveListCandle.addAll(t2);
            case P_1440:
                List<CandleDay> t3 = candleService.getDay(market, longPeriod);
                moveListCandle.addAll(t3);
        }
        return moveListCandle;
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
//        orderService.callOrderBidByMarket(market, ApplicationUtil.toNumberString(bidPrice));
        tradeEvent.bid(market, currentPrice, bidPrice);
    }

    private void doAsk(double currentPrice, double balance, AskReason maDown) {
//        orderService.callOrderAskByMarket(market, ApplicationUtil.toNumberString(balance));
        tradeEvent.ask(market, balance, currentPrice, maDown);
        tradeCompleteOfPeriod = true;
        highYield = 0;
    }

    private double getYield(Candle candle, Optional<Account> account) {
        double avgPrice = AccountService.getAvgBuyPrice(account).get().doubleValue();
        double diff = candle.getTradePrice() - avgPrice;
        return diff / avgPrice;
    }

}
