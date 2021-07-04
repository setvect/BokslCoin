package com.setvect.bokslcoin.autotrading.algorithm.mais;

import com.setvect.bokslcoin.autotrading.algorithm.AskReason;
import com.setvect.bokslcoin.autotrading.algorithm.CoinTrading;
import com.setvect.bokslcoin.autotrading.algorithm.CommonTradeHelper;
import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.TradePeriod;
import com.setvect.bokslcoin.autotrading.exchange.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.OrderService;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import com.setvect.bokslcoin.autotrading.util.MathUtil;
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
import java.util.stream.Collectors;

/**
 * 이평선 반전 전략
 */
@Service("mais")
@Slf4j
@RequiredArgsConstructor
public class MabsService implements CoinTrading {
    private final AccountService accountService;
    private final CandleService candleService;
    private final TradeEvent tradeEvent;
    private final OrderService orderService;
    private final SlackMessageService slackMessageService;

    /**
     * 과거 이평선 비교 범위
     */
    private static final int COMPARISON_RANGE = 5;

    /**
     * 매수, 매도 대상 코인
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mais.market}")
    private String market;

    /**
     * 총 현금을 기준으로 투자 비율
     * 1은 100%, 0.5은 50% 투자
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mais.investRatio}")
    private double investRatio;

    /**
     * 매매 주기
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mais.tradePeriod}")
    private TradePeriod tradePeriod;

    /**
     * 상승 매수률
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mais.upBuyRate}")
    private double upBuyRate;

    /**
     * 하락 매도률
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mais.downSellRate}")
    private double downSellRate;

    /**
     * 기준 이동평균 기간
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mais.basePeriod}")
    private int basePeriod;
    /**
     * 슬랙 메시지 발송 시간
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mais.slackTime}")
    private String slackTime;

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

    private boolean messageSend = false;


    @Override
    public void apply() {
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
            messageSend = false;
        }

        List<Candle> candleList = CommonTradeHelper.getCandles(candleService, market, tradePeriod, basePeriod + COMPARISON_RANGE);
        List<Double> priceValues = candleList.stream().map(c -> c.getTradePrice()).collect(Collectors.toList());
        double currentMa = MathUtil.getAverage(priceValues, 0, basePeriod);
        List<Double> beforePriceMa = MathUtil.getAverageValues(priceValues, 1, basePeriod, COMPARISON_RANGE);
        double maxPrice = MathUtil.getContinuesMax(beforePriceMa);
        double minPrice = MathUtil.getContinuesMin(beforePriceMa);

        Optional<Account> coinAccount = accountService.getAccount(market);
        BigDecimal coinBalance = AccountService.getBalance(coinAccount);

        tradeEvent.check(candle, currentMa);

        if (currentMa == 0) {
            log.warn("이동평균계산을 위한 시세 데이터가 부족합니다.");
            return;
        }

        log.debug(String.format("KST:%s, UTC: %s, 매매기준 주기: %s, 현재가: %,.2f, MA_%d: %,.2f, MA_MAX: %,.2f(%.2f%%), MA_MIN: %,.2f(%.2f%%)",
                DateUtil.formatDateTime(nowKst),
                DateUtil.formatDateTime(nowUtc),
                tradePeriod,
                candle.getTradePrice()),
                currentMa,
                maxPrice,
                MathUtil.getYield(currentMa, maxPrice) * 100,
                minPrice,
                MathUtil.getYield(currentMa, minPrice) * 100
        );

        double balance = coinBalance.doubleValue();

        // 코인을 매수 했다면 매도 조건 판단
        if (balance > 0.00001) {
            double rate = getYield(candle, coinAccount);
            highYield = Math.max(highYield, rate);
            double sellTargetPrice = maxPrice - maxPrice * downSellRate;

            Account account = coinAccount.get();
            String message1 = String.format("매입단가: %,.2f, 투자금: %,.0f, 수익률: %.2f%%, 최고 수익률: %.2f%%, 매도목표가: %,.2f",
                    Double.valueOf(account.getAvgBuyPrice()),
                    getInvestCash(account),
                    rate * 100,
                    highYield * 100,
                    sellTargetPrice
            );
            log.debug(message1);

            // 매도 조건: 현재 이평이 직전 이평선 가격 보다 낮은 경우 매도
            boolean isSell = currentMa <= sellTargetPrice;
            String message2 = String.format("매도 조건: 현재 이평 <= 과거이평 최대값 - 과거이평 최대값 * 하락매도률(%.2f%%), %,.2f <= %,.2f ---> %s", downSellRate * 100, currentMa, sellTargetPrice, isSell);
            log.debug(message2);
            sendSlack(message1 + "\n" + message2, candle.getCandleDateTimeKst());

            if (isSell) {
                doAsk(candle.getTradePrice(), balance, AskReason.MA_DOWN);
            }
        }
        // 매수 조건 판단
        else if (!tradeCompleteOfPeriod) {
            double buyTargetPrice = minPrice + minPrice * upBuyRate;

            //매수 조건: 현재 이평이 직전 이평가격 보다 클 경우
            boolean isBuy = currentMa >= buyTargetPrice;
            String message = String.format("매수 조건: 현재 이평 >= 과거이평 최소값 + 과거이평 최소값 * 상승매수률(%.2f%%), %,.2f <= %,.2f ---> %s\"", upBuyRate * 100, currentMa, buyTargetPrice);
            sendSlack(message, candle.getCandleDateTimeKst());
            log.debug(message);
            if (isBuy) {
                doBid(currentPrice);
            }
        }
    }

    private void sendSlack(String message, LocalDateTime kst) {
        LocalTime time = DateUtil.getLocalTime(slackTime, "HH:mm");
        // 하루에 한번씩만 보냄
        if (messageSend) {
            return;
        }

        // 정해진 시간에 메시지 보냄
        if (time.getHour() == kst.getHour() && time.getMinute() == kst.getMinute()) {
            slackMessageService.sendMessage(message);
            messageSend = true;
        }
    }

    private int getCurrentPeriod(LocalDateTime nowUtc) {
        int dayHourMinuteSum = nowUtc.getDayOfMonth() * 1440 + nowUtc.getHour() * 60 + nowUtc.getMinute();
        int currentPeriod = dayHourMinuteSum / tradePeriod.getTotal();
        return currentPeriod;
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

    private void doAsk(double currentPrice, double balance, AskReason maDown) {
        orderService.callOrderAskByMarket(market, ApplicationUtil.toNumberString(balance));
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
