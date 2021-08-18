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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 이평선 반전 전략 + 멀티 코인
 * 코인별 동일한 현금 비율로 매매를 수행한다.
 */
@Service("maisMulti")
@Slf4j
@RequiredArgsConstructor
public class MaisMultiService implements CoinTrading {
    private final AccountService accountService;
    private final CandleService candleService;
    private final TradeEvent tradeEvent;
    private final OrderService orderService;
    private final SlackMessageService slackMessageService;

    /**
     * 과거 이평선 비교 범위
     */
    private static final int COMPARISON_RANGE = 10;

    /**
     * 매수, 매도 대상 코인
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.maisMulti.markets}")
    private List<String> markets;

    /**
     * 최대 코인 매매 갯수
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.maisMulti.maxBuyCount}")
    private int maxBuyCount;

    /**
     * 총 현금을 기준으로 투자 비율
     * 1은 100%, 0.5은 50% 투자
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.maisMulti.investRatio}")
    private double investRatio;

    /**
     * 손절 매도
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.maisMulti.loseStopRate}")
    private double loseStopRate;

    /**
     * 매매 주기
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.maisMulti.tradePeriod}")
    private TradePeriod tradePeriod;

    /**
     * 상승 매수률
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.maisMulti.upBuyRate}")
    private double upBuyRate;

    /**
     * 하락 매도률
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.maisMulti.downSellRate}")
    private double downSellRate;

    /**
     * 기준 이동평균 기간
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.maisMulti.maPeriod}")
    private int maPeriod;
    /**
     * 슬랙 메시지 발송 시간
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.maisMulti.slackTime}")
    private String slackTime;

    /**
     * 해당 기간에 매매 여부 완료 여부
     * value: 코인 예) KRW-BTC, KRW-ETH, ...
     */
    private Set<String> tradeCompleteOfPeriod = new HashSet<>();

    /**
     * 코인별 일일 메시지 전달 여부
     * value: 코인 예) KRW-BTC, KRW-ETH, ...
     */
    private Set<String> messageSend = new HashSet<>();

    private int periodIdx = -1;

    /**
     * 매수 이후 최고 수익률
     */
    @Getter
    private Map<String, Double> highYield = new HashMap<>();
    /**
     * 매수 이후 최저 수익률
     */
    @Getter
    private Map<String, Double> lowYield = new HashMap<>();

    @Override
    public void apply() {
        // 아무 코인이나 분봉으로 조회하여 매매 주기가 변경되었는지 확인
        CandleMinute candleCheck = candleService.getMinute(1, markets.get(0));
        ZonedDateTime nowUtcZoned = candleCheck.getCandleDateTimeUtc().atZone(ZoneId.of("UTC"));
        LocalDateTime nowUtc = nowUtcZoned.toLocalDateTime();
        int currentPeriod = getCurrentPeriod(nowUtc);

        // 새로운 날짜면 매매 다시 초기화
        if (periodIdx != currentPeriod) {
            tradeEvent.newPeriod(candleCheck);
            periodIdx = currentPeriod;
            tradeCompleteOfPeriod.clear();
            messageSend.clear();
        }


        Map<String, Account> coinAccount = accountService.getMyAccountBalance();
        Account krw = coinAccount.get("KRW");
        BigDecimal cash = BigDecimal.valueOf(krw.getBalanceValue());

        int allowBuyCount = Math.min(this.maxBuyCount, markets.size());
        // 이미 매수한 코인 갯수
        int buyCount = (int) markets.stream().filter(p -> coinAccount.get(p) != null).count();
        int rate = allowBuyCount - buyCount;

        double buyCash = 0;
        if (rate > 0) {
            buyCash = (cash.doubleValue() * investRatio) / rate;
        }

        for (String market : markets) {
            candleCheck = candleService.getMinute(1, market);
            if (candleCheck == null) {
                log.debug("[{}] 현재 시세 데이터가 없습니다.", market);
                continue;
            }
            log.debug(String.format("[%s] 현재 가격:%,.2f", candleCheck.getMarket(), candleCheck.getTradePrice()));

            Account account = coinAccount.get(market);
            List<Candle> candleList = CommonTradeHelper.getCandles(candleService, market, tradePeriod, maPeriod + COMPARISON_RANGE);
            List<Double> priceValues = candleList.stream().map(c -> c.getTradePrice()).collect(Collectors.toList());
            try {
                double currentMa = MathUtil.getAverage(priceValues, 0, maPeriod);
                if (currentMa == 0) {
                    log.warn("이동평균계산을 위한 시세 데이터가 부족합니다.");
                    return;
                }
                checkMa(candleList);
            } catch (Exception e) {
                log.error(e.getMessage());
                return;
            }

            if (account == null && !tradeCompleteOfPeriod.contains(market) && buyCash != 0) {
                buyCheck(buyCash, candleList);
            } else if (account != null) {
                sellCheck(account, candleList);
            } else {
                log.debug(String.format("[%s] 현재 가격:%,.2f", candleCheck.getMarket(), candleCheck.getTradePrice()));
            }
        }
    }

    /**
     * 시세 체크
     *
     * @param candleList
     */
    private void checkMa(List<Candle> candleList) {
        List<Double> priceValues = candleList.stream().map(c -> c.getTradePrice()).collect(Collectors.toList());
        double currentMa = MathUtil.getAverage(priceValues, 0, maPeriod);
        List<Double> beforePriceMa = MathUtil.getAverageValues(priceValues, 1, maPeriod, COMPARISON_RANGE);
        double maxMa = MathUtil.getContinuesMax(beforePriceMa);
        double minMa = MathUtil.getContinuesMin(beforePriceMa);
        Candle candle = candleList.get(0);
        tradeEvent.check(candle, currentMa, maxMa, minMa);

        log.debug(String.format("KST:%s, UTC: %s, 매매기준 주기: %s, 현재가: %,.2f, MA_%d: %,.2f, MA_MAX: %,.2f(%.2f%%), MA_MIN: %,.2f(%.2f%%)",
                DateUtil.formatDateTime(candle.getCandleDateTimeKst()),
                DateUtil.formatDateTime(candle.getCandleDateTimeUtc()),
                tradePeriod,
                candle.getTradePrice(),
                maPeriod,
                currentMa,
                maxMa,
                MathUtil.getYield(currentMa, maxMa) * 100,
                minMa,
                MathUtil.getYield(currentMa, minMa) * 100
        ));

    }

    /**
     * 조건이 만족하면 매수 수행
     *
     * @param cash       매수에 사용될 현금
     * @param candleList
     */
    private void buyCheck(double cash, List<Candle> candleList) {
        List<Double> priceValues = candleList.stream().map(c -> c.getTradePrice()).collect(Collectors.toList());
        double currentMa = MathUtil.getAverage(priceValues, 0, maPeriod);
        List<Double> beforePriceMa = MathUtil.getAverageValues(priceValues, 1, maPeriod, COMPARISON_RANGE);
        double minMa = MathUtil.getContinuesMin(beforePriceMa);

        double buyTargetPrice = minMa + minMa * upBuyRate;

        Candle candle = candleList.get(0);
        String market = candle.getMarket();

        //매수 조건: 현재 이평이 직전 이평가격 보다 클 경우
        boolean isBuy = currentMa >= buyTargetPrice;
        String message = String.format("[%s] 매수 조건: MA_%d >= MA_MIN + MA_MIN * 상승매수률(%.2f%%) ==> %,.2f <= %,.2f ==> %s", market, maPeriod, upBuyRate * 100, currentMa, buyTargetPrice, isBuy);
        log.debug(message);

        sendSlackDaily(market, message);
        log.debug(message);
        if (isBuy) {
            doBid(market, candle.getTradePrice(), cash);
        }
    }

    /**
     * 조건이 만족하면 매도
     *
     * @param account
     */
    private void sellCheck(Account account, List<Candle> candleList) {
        List<Double> priceValues = candleList.stream().map(c -> c.getTradePrice()).collect(Collectors.toList());
        double currentMa = MathUtil.getAverage(priceValues, 0, maPeriod);
        List<Double> beforePriceMa = MathUtil.getAverageValues(priceValues, 1, maPeriod, COMPARISON_RANGE);
        double maxMa = MathUtil.getContinuesMax(beforePriceMa);

        String market = account.getMarket();

        Candle candle = candleList.get(0);
        double rate = getYield(candle, account);

        double maxHighYield = Math.max(highYield.getOrDefault(market, 0.0), rate);
        highYield.put(market, maxHighYield);
        tradeEvent.highYield(candle.getMarket(), maxHighYield);

        double minLowYield = Math.min(lowYield.getOrDefault(market, 0.0), rate);
        lowYield.put(market, minLowYield);
        tradeEvent.lowYield(candle.getMarket(), minLowYield);


        String message1 = String.format("[%s] 현재가: %,.2f, 매입단가: %,.2f, 투자금: %,.0f, 수익율: %.2f%%, 최고 수익률: %.2f%%, 최저 수익률: %.2f%%",
                candle.getMarket(),
                candle.getTradePrice(),
                account.getAvgBuyPriceValue(),
                account.getInvestCash(),
                rate * 100,
                highYield.get(market) * 100,
                lowYield.get(market) * 100
        );
        log.debug(message1);


        // 매도 조건: 현재 이평이 직전 이평선 가격 보다 낮은 경우 매도
        double sellTargetPrice = maxMa - maxMa * downSellRate;
        boolean isSell = currentMa <= sellTargetPrice;

        String message2 = String.format("[%s] 매도 조건: MA_%d <= MA_MAX - MA_MAX * 하락매도률(%.2f%%) ==> %,.2f <= %,.2f ==> %s",
                candle.getMarket(), maPeriod, downSellRate * 100, currentMa, sellTargetPrice, isSell);
        log.debug(message2);

        sendSlackDaily(market, message1 + "\n" + message2);

        if (isSell) {
            slackMessageService.sendMessage(message1);
            doAsk(market, candle.getTradePrice(), account.getBalanceValue(), AskReason.MA_DOWN);
        }
    }

    private boolean isEnough(double maShort, double maLong) {
        return maShort == 0 || maLong == 0;
    }


    private int getCurrentPeriod(LocalDateTime nowUtc) {
        int dayHourMinuteSum = nowUtc.getDayOfMonth() * 1440 + nowUtc.getHour() * 60 + nowUtc.getMinute();
        int currentPeriod = dayHourMinuteSum / tradePeriod.getTotal();
        return currentPeriod;
    }


    private void sendSlackDaily(String market, String message) {
        // 하루에 한번씩만 보냄
        if (messageSend.contains(market)) {
            return;
        }
        LocalTime kst = LocalTime.now();

        LocalTime time = DateUtil.getLocalTime(slackTime, "HH:mm");
        // 정해진 시간에 메시지 보냄
        if (time.getHour() == kst.getHour() && time.getMinute() == kst.getMinute()) {
            slackMessageService.sendMessage(message);
            messageSend.add(market);
        }
    }

    private void doBid(String market, double tradePrice, double bidPrice) {
        orderService.callOrderBidByMarket(market, ApplicationUtil.toNumberString(bidPrice));
        tradeEvent.bid(market, tradePrice, bidPrice);
    }

    private void doAsk(String market, double currentPrice, double balance, AskReason maDown) {
        orderService.callOrderAskByMarket(market, ApplicationUtil.toNumberString(balance));
        tradeEvent.ask(market, balance, currentPrice, maDown);
        highYield.put(market, 0.0);
        lowYield.put(market, 0.0);
        tradeCompleteOfPeriod.add(market);
    }

    private double getYield(Candle candle, Account account) {
        double avgPrice = account.getAvgBuyPriceValue();
        double diff = candle.getTradePrice() - avgPrice;
        return diff / avgPrice;
    }
}
