package com.setvect.bokslcoin.autotrading.algorithm.mabsss;

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

/**
 * 이평선 돌파 전략 + 분할 매도
 */
@Service("mabsss")
@Slf4j
@RequiredArgsConstructor
public class MabsSplitSellService implements CoinTrading {
    private final AccountService accountService;
    private final CandleService candleService;
    private final TradeEvent tradeEvent;
    private final OrderService orderService;
    private final SlackMessageService slackMessageService;

    /**
     * 매수, 매도 대상 코인
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsss.market}")
    private String market;

    /**
     * 총 현금을 기준으로 투자 비율
     * 1은 100%, 0.5은 50% 투자
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsss.investRatio}")
    private double investRatio;

    /**
     * 매매 주기
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsss.tradePeriod}")
    private TradePeriod tradePeriod;

    /**
     * 상승 매수률
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsss.upBuyRate}")
    private double upBuyRate;

    /**
     * 하락 매도률
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsss.downSellRate}")
    private double downSellRate;

    /**
     * 단기 이동평균 기간
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsss.shortPeriod}")
    private int shortPeriod;

    /**
     * 장기 이동평균 기간
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsss.longPeriod}")
    private int longPeriod;

    /**
     * 슬랙 메시지 발송 시간
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsss.slackTime}")
    private String slackTime;

    /**
     * 분할 매도 카운트
     * TODO 설정으로 이동
     */
    private int splitCount = 0;

    /**
     * 한번에 매도할 비율
     * TODO 설정으로 이동
     */
    private double SPLIT_RATE = 0.1;
    /**
     * 최소 분할 매도율. 수익률이 해당 값을 초과 올라가야 분할 매도함
     * TODO 설정으로 이동
     */
    private double minimumYield = 0.01;

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

    /**
     * 현제 매매 주기에서 분할 매도를 했는가?
     */
    private boolean periodSell = false;
    /**
     * 한번에 분할 매도할 값.
     */
    private double splitBalance = 0;


    @Override
    public void apply() {
        List<Candle> candleList = CommonTradeHelper.getCandles(candleService, market, tradePeriod, longPeriod);
        double maShort = CommonTradeHelper.getMa(candleList, shortPeriod);
        double maLong = CommonTradeHelper.getMa(candleList, longPeriod);

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
            messageSend = false;
            periodSell = false;
        }

        tradeEvent.check(candle, maShort, maLong);

        if (maShort == 0 || maLong == 0) {
            log.warn("이동평균계산을 위한 시세 데이터가 부족합니다.");
            return;
        }

        log.debug(String.format("KST:%s, UTC: %s, 매매기준 주기: %s, 현재가: %,.2f, MA_%d: %,.2f, MA_%d: %,.2f, 단기-장기 차이: %,.2f(%.2f%%)",
                DateUtil.formatDateTime(nowKst),
                DateUtil.formatDateTime(nowUtc),
                tradePeriod,
                candle.getTradePrice(),
                shortPeriod, maShort,
                longPeriod, maLong,
                maShort - maLong, MathUtil.getYield(maShort, maLong) * 100));

        double balance = coinBalance.doubleValue();
        // 코인을 매수 했다면 매도 조건 판단
        if (balance > 0.00001) {
            // 프로그램이 다시 시작되어 분할 매도 값이 없어진 경우 현재 가지고 있는 값을 기준으로 계산
            if (splitBalance == 0) {
                splitBalance = balance * SPLIT_RATE;
            }

            double rate = getYield(candle, coinAccount);
            if (highYield < rate) {
                highYield = rate;

                if (!periodSell && highYield > minimumYield && splitCount < 9) {
                    doAsk(candle.getTradePrice(), splitBalance, AskReason.SPLIT);
                    periodSell = true;
                    splitCount++;
                    return;
                }
            }
            double sellTargetPrice = maShort + maShort * downSellRate;

            Account account = coinAccount.get();
            String message1 = String.format("매입단가: %,.2f, 투자금: %,.0f, 수익율: %.2f%%, 최고 수익률: %.2f%%",
                    Double.valueOf(account.getAvgBuyPrice()),
                    getInvestCash(account),
                    rate * 100,
                    highYield * 100
            );
            log.debug(message1);
            // 분할 매도가 이루어 졌다면 해당 주기에는 매도하지 않음
            if (periodSell) {
                return;
            }

            // 장기이평 >= (단기이평 + 단기이평 * 하락매도률)
            boolean isSell = maLong >= sellTargetPrice;
            String message2 = String.format("매도 조건: 장기이평(%d) >= 단기이평(%d) + 단기이평(%d) * 하락매도률(%.2f%%), %,.2f >= %,.2f ---> %s", longPeriod, shortPeriod, shortPeriod, downSellRate * 100, maLong, sellTargetPrice, isSell);
            log.debug(message2);

            sendSlack(message1 + "\n" + message2, candle.getCandleDateTimeKst());

            // 보유 물량 전체 매도
            if (isSell) {
                doAsk(candle.getTradePrice(), balance, AskReason.MA_DOWN);
                splitCount = 0;
                tradeCompleteOfPeriod = true;
                highYield = 0;
            }
        }
        // 매수 조건 판단
        else if (!tradeCompleteOfPeriod) {
            double buyTargetPrice = maLong + maLong * upBuyRate;

            //(장기이평 + 장기이평 * 상승매수률) <= 단기이평
            boolean isBuy = buyTargetPrice <= maShort;
            String message = String.format("매수 조건: 장기이평(%d) + 장기이평(%d) * 상승매수률(%.2f%%) <= 단기이평(%d), %,.2f <= %,.2f ---> %s", longPeriod, longPeriod, upBuyRate * 100, shortPeriod, buyTargetPrice, maShort, isBuy);
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

    private void doAsk(double currentPrice, double balance, AskReason askReason) {
        orderService.callOrderAskByMarket(market, ApplicationUtil.toNumberString(balance));
        tradeEvent.ask(market, balance, currentPrice, askReason);
    }

    private double getYield(Candle candle, Optional<Account> account) {
        double avgPrice = AccountService.getAvgBuyPrice(account).get().doubleValue();
        double diff = candle.getTradePrice() - avgPrice;
        return diff / avgPrice;
    }

}
