package com.setvect.bokslcoin.autotrading.algorithm.mabs;

import com.setvect.bokslcoin.autotrading.algorithm.AskPriceRange;
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
import com.setvect.bokslcoin.autotrading.record.entity.AssetHistoryEntity;
import com.setvect.bokslcoin.autotrading.record.entity.TradeEntity;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import com.setvect.bokslcoin.autotrading.record.repository.AssetHistoryRepository;
import com.setvect.bokslcoin.autotrading.record.repository.TradeRepository;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import com.setvect.bokslcoin.autotrading.util.MathUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 이평선 돌파 전략 + 멀티 코인
 * 코인별 동일한 현금 비율로 매매를 수행한다.
 */
@Service("mabsMulti")
@Slf4j
@RequiredArgsConstructor
public class MabsMultiService implements CoinTrading {
    /**
     * 매수/매도 시 즉각적인 매매를 위해 호가보다 상단 또는 하단에 주문을 넣는 퍼센트
     */
    private static final double DIFF_RATE = 0.015;
    /**
     * 최소 매매 금액
     */
    private static final int MINIMUM_BUY_CASH = 5_000;
    private final AccountService accountService;
    private final CandleService candleService;
    private final TradeEvent tradeEvent;
    private final OrderService orderService;
    private final SlackMessageService slackMessageService;
    private final AssetHistoryRepository assetHistoryRepository;
    private final TradeRepository tradeRepository;

    /**
     * 매수, 매도 대상 코인
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsMulti.markets}")
    private List<String> markets;

    /**
     * 최대 코인 매매 갯수
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsMulti.maxBuyCount}")
    private int maxBuyCount;

    /**
     * 총 현금을 기준으로 투자 비율
     * 1은 100%, 0.5은 50% 투자
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsMulti.investRatio}")
    private double investRatio;

    /**
     * 손절 매도
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsMulti.loseStopRate}")
    private double loseStopRate;

    /**
     * 매매 주기
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsMulti.tradePeriod}")
    private TradePeriod tradePeriod;

    /**
     * 상승 매수률
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsMulti.upBuyRate}")
    private double upBuyRate;

    /**
     * 하락 매도률
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsMulti.downSellRate}")
    private double downSellRate;

    /**
     * 단기 이동평균 기간
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsMulti.shortPeriod}")
    private int shortPeriod;

    /**
     * 장기 이동평균 기간
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsMulti.longPeriod}")
    private int longPeriod;

    /**
     * 프로그램을 시작하자마자 매수하는걸 방지하기 위함.
     * true: 직전 이동평균을 감지해 새롭게 돌파 했을 때만 매수
     * false: 프로그램 시작과 동시에 매수 조건이 만족하면 매수, 고가에 매수할 가능성 있음
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.mabsMulti.newMasBuy}")
    private boolean newMasBuy;

    /**
     * 해당 기간에 매매 여부 완료 여부
     * value: 코인 예) KRW-BTC, KRW-ETH, ...
     */
    private final Set<String> tradeCompleteOfPeriod = new HashSet<>();

    /**
     * 가격 저장
     */
    private boolean assetCoinSave = false;

    private int periodIdx = -1;

    /**
     * 매수 이후 최고 수익률
     */
    @Getter
    private final Map<String, Double> highYield = new HashMap<>();
    /**
     * 매수 이후 최저 수익률
     */
    @Getter
    private final Map<String, Double> lowYield = new HashMap<>();

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
            assetCoinSave = false;
        }

        Map<String, Account> coinAccount = accountService.getMyAccountBalance();
        Account krw = coinAccount.get("KRW");
        BigDecimal cash = BigDecimal.valueOf(krw.getBalanceValue());

        // 이미 매수한 코인 갯수
        int allowBuyCount = Math.min(this.maxBuyCount, markets.size());
        int buyCount = (int) markets.stream().filter(p -> coinAccount.get(p) != null).count();
        int rate = allowBuyCount - buyCount;

        double buyCash = 0;
        if (rate > 0) {
            buyCash = (cash.doubleValue() * investRatio) / rate;
        }

        // 코인별 마지막 캔들
        Map<String, Candle> lastCandle = new HashMap<>();

        List<String> priceCheckMessageList = new ArrayList<>();

        for (String market : markets) {
            List<Candle> candleList = CommonTradeHelper.getCandles(candleService, market, tradePeriod, longPeriod + 1);
            if (candleList.isEmpty()) {
                log.debug("[{}] 현재 시세 데이터가 없습니다.", market);
                continue;
            }
            Account account = coinAccount.get(market);

            if (candleList.size() < longPeriod + 1) {
                log.debug("[{}] 이동평균계산을 위한 시세 데이터가 부족합니다.", market);
                continue;
            }

            String checkMessage = checkMa(candleList);
            priceCheckMessageList.add(checkMessage);

            lastCandle.put(market, candleList.get(0));
            if (account == null && !tradeCompleteOfPeriod.contains(market) && buyCash != 0) {
                buyCheck(buyCash, candleList);
            } else if (account != null) {
                sellCheck(account, candleList);
            }
        }

        if (!assetCoinSave) {
            List<AssetHistoryEntity> rateByCoin = writeCurrentAssetRate(coinAccount, lastCandle, candleCheck.getCandleDateTimeKst());
            sendCurrentStatus(priceCheckMessageList, rateByCoin);
        }
        assetCoinSave = true;
    }

    /**
     * 현재 시세정보와 투자 수익률 슬렉으로 전달
     *
     * @param currentPriceMessage 코인별 시세 정보
     * @param rateByCoin          코인 투자 수익률
     */
    private void sendCurrentStatus(List<String> currentPriceMessage, List<AssetHistoryEntity> rateByCoin) {
        String priceMessage = StringUtils.join(currentPriceMessage, "\n");
        String rateMessage = rateByCoin.stream()
                .filter(p -> !p.getCurrency().equals("KRW"))
                .map(p -> String.format("[%s] 수익률: %,.2f", p.getCurrency(), p.getYield()))
                .collect(Collectors.joining("\n"));

        slackMessageService.sendMessage(StringUtils.join("\n-----------\n", priceMessage, rateMessage));
    }

    /**
     * 현재 보유중인 코인 및 현금 수익률 저장
     *
     * @param accounts   Key: 계좌이름,Value: 계좌 정보
     * @param lastCandle 마지막 캔들
     * @param regDate    등록일
     * @return 자산별 수익률 정보
     */
    private List<AssetHistoryEntity> writeCurrentAssetRate(Map<String, Account> accounts, Map<String, Candle> lastCandle, LocalDateTime regDate) {
        List<AssetHistoryEntity> accountHistoryList = accounts.entrySet().stream().map(entity -> {
            Account account = entity.getValue();
            AssetHistoryEntity assetHistory = new AssetHistoryEntity();
            assetHistory.setCurrency(entity.getKey());
            double price = entity.getKey().equals("KRW") ? account.getBalanceValue() : account.getInvestCash();
            assetHistory.setPrice(price);

            Candle candle = lastCandle.get(entity.getKey());
            if (candle != null) {
                assetHistory.setYield(getYield(candle, account));
            }
            assetHistory.setRegDate(regDate);
            return assetHistory;
        }).collect(Collectors.toList());

        assetHistoryRepository.saveAll(accountHistoryList);
        return accountHistoryList;
    }

    /**
     * 시세 체크
     *
     * @param candleList 캔들 값
     * @return 시세 체크
     */
    private String checkMa(List<Candle> candleList) {
        double maShort = CommonTradeHelper.getMa(candleList, shortPeriod);
        double maLong = CommonTradeHelper.getMa(candleList, longPeriod);
        Candle candle = candleList.get(0);
        tradeEvent.check(candle, maShort, maLong);
        String message = String.format("[%s] 단기-장기 차이: %,.2f(%.2f%%), 현재가: %,.2f, MA_%d: %,.2f, MA_%d: %,.2f",
                candle.getMarket(),
                maShort - maLong,
                MathUtil.getYield(maShort, maLong) * 100,
                candle.getTradePrice(),
                shortPeriod, maShort,
                longPeriod, maLong
        );
        log.debug(message);
        return String.format("[%s] %.2f%%, %,.0f",
                candle.getMarket(),
                MathUtil.getYield(maShort, maLong) * 100,
                candle.getTradePrice()
        );
    }

    /**
     * 조건이 만족하면 매수 수행
     *
     * @param cash       매수에 사용될 현금
     * @param candleList 캔들
     */
    private void buyCheck(double cash, List<Candle> candleList) {
        double maShort = CommonTradeHelper.getMa(candleList, shortPeriod);
        double maLong = CommonTradeHelper.getMa(candleList, longPeriod);

        Candle candle = candleList.get(0);
        double buyTargetPrice = maLong + maLong * upBuyRate;
        String market = candle.getMarket();

        //(장기이평 + 장기이평 * 상승매수률) <= 단기이평
        boolean isBuy = buyTargetPrice <= maShort;

        if (isBuy && cash >= MINIMUM_BUY_CASH) {
            // 직전 이동평균을 감지해 새롭게 돌파 했을 때만 매수
            boolean isBeforeBuy = isBeforeBuy(candleList);
            if (isBeforeBuy && newMasBuy) {
                log.debug("[{}] 매수 안함. 새롭게 이동평균을 돌파할 때만 매수합니다.", candle.getMarket());
                return;
            }
            doBid(market, candle.getTradePrice(), cash);
        }
    }

    /**
     * 조건이 만족하면 매도
     *
     * @param account 계좌
     */
    private void sellCheck(Account account, List<Candle> candleList) {
        String market = account.getMarket();

        Candle candle = candleList.get(0);
        double rate = getYield(candle, account);

        double maxHighYield = Math.max(highYield.getOrDefault(market, 0.0), rate);
        highYield.put(market, maxHighYield);
        tradeEvent.highYield(candle.getMarket(), maxHighYield);

        double minLowYield = Math.min(lowYield.getOrDefault(market, 0.0), rate);
        lowYield.put(market, minLowYield);
        tradeEvent.lowYield(candle.getMarket(), minLowYield);

        double maShort = CommonTradeHelper.getMa(candleList, shortPeriod);
        double maLong = CommonTradeHelper.getMa(candleList, longPeriod);

        String message1 = String.format("[%s] 현재가: %,.2f, 매입단가: %,.2f, 투자금: %,.0f, 수익률: %.2f%%, 최고 수익률: %.2f%%, 최저 수익률: %.2f%%",
                candle.getMarket(),
                candle.getTradePrice(),
                account.getAvgBuyPriceValue(),
                account.getInvestCash(),
                rate * 100,
                highYield.get(market) * 100,
                lowYield.get(market) * 100
        );
        log.debug(message1);

        // 장기이평 >= (단기이평 + 단기이평 * 하락매도률)
        double sellTargetPrice = maShort + maShort * downSellRate;
        boolean isSell = maLong >= sellTargetPrice;

        if (isSell || loseStopRate < -rate) {
            slackMessageService.sendMessage(message1);
            doAsk(market, candle.getTradePrice(), account.getBalanceValue(), rate);
        }
    }

    private int getCurrentPeriod(LocalDateTime nowUtc) {
        int dayHourMinuteSum = nowUtc.getDayOfMonth() * 1440 + nowUtc.getHour() * 60 + nowUtc.getMinute();
        return dayHourMinuteSum / tradePeriod.getTotal();
    }

    /**
     * 매수
     *
     * @param market     코인 종류
     * @param tradePrice 코인 가격
     * @param bidPrice   매수 금액
     */
    private void doBid(String market, double tradePrice, double bidPrice) {
        // 매수 가격, 높은 가격으로 매수(시장가 효과)
        double fitPrice = AskPriceRange.askPrice(tradePrice + tradePrice * DIFF_RATE);

        // 매수 수량
        String volume = ApplicationUtil.toNumberString(bidPrice / fitPrice);

        String price = ApplicationUtil.toNumberString(fitPrice);
        orderService.callOrderBid(market, volume, price);

        TradeEntity trade = new TradeEntity();
        trade.setMarket(market);
        trade.setTradeType(TradeType.BUY);
        // 매수 호가 보다 높게 체결 될 가능성이 있기 때문에 오차가 있음
        double amount = tradePrice * Double.parseDouble(volume);
        trade.setAmount(amount);
        trade.setUnitPrice(tradePrice);
        trade.setRegDate(LocalDateTime.now());
        tradeRepository.save(trade);

        tradeEvent.bid(market, tradePrice, bidPrice);
    }


    /**
     * 매도
     *
     * @param market       코인 종류
     * @param currentPrice 코인 가격
     * @param balance      코인 주문량
     * @param yield        수익률
     */
    private void doAsk(String market, double currentPrice, double balance, double yield) {
        // 매도 가격, 낮은 가격으로 매도(시장가 효과)
        double fitPrice = AskPriceRange.askPrice(currentPrice - currentPrice * DIFF_RATE);
        orderService.callOrderAsk(market, ApplicationUtil.toNumberString(balance), ApplicationUtil.toNumberString(fitPrice));

        TradeEntity trade = new TradeEntity();
        trade.setMarket(market);
        trade.setTradeType(TradeType.SELL);
        // 매도 호가 보다 낮게 체결 될 가능성이 있기 때문에 오차가 있음
        trade.setAmount(currentPrice * balance);
        trade.setUnitPrice(currentPrice);
        trade.setRegDate(LocalDateTime.now());
        trade.setYield(yield);
        tradeRepository.save(trade);

        tradeEvent.ask(market, balance, currentPrice, AskReason.MA_DOWN);
        highYield.put(market, 0.0);
        lowYield.put(market, 0.0);
        tradeCompleteOfPeriod.add(market);
    }


    /**
     * @param candleList 캔들
     * @return 이동 평균에서 직전 매수 조건 이면 true, 아니면 false
     */
    private boolean isBeforeBuy(List<Candle> candleList) {
        // 한단계전에 매수 조건이였는지 확인
        List<Candle> beforeCandleList = candleList.subList(1, candleList.size());
        double maShortBefore = CommonTradeHelper.getMa(beforeCandleList, shortPeriod);
        double maLongBefore = CommonTradeHelper.getMa(beforeCandleList, longPeriod);
        double buyTargetPrice = maLongBefore + maLongBefore * upBuyRate;
        //(장기이평 + 장기이평 * 상승매수률) <= 단기이평
        return buyTargetPrice <= maShortBefore;
    }


    private double getYield(Candle candle, Account account) {
        double avgPrice = account.getAvgBuyPriceValue();
        double diff = candle.getTradePrice() - avgPrice;
        return diff / avgPrice;
    }
}
