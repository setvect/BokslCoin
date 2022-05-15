package com.setvect.bokslcoin.autotrading.algorithm.mabs;

import com.setvect.bokslcoin.autotrading.algorithm.AskPriceRange;
import com.setvect.bokslcoin.autotrading.algorithm.AskReason;
import com.setvect.bokslcoin.autotrading.algorithm.CoinTrading;
import com.setvect.bokslcoin.autotrading.algorithm.CommonTradeHelper;
import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.websocket.TradeResult;
import com.setvect.bokslcoin.autotrading.exchange.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.OrderService;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.OrderHistory;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import com.setvect.bokslcoin.autotrading.record.entity.AssetHistoryEntity;
import com.setvect.bokslcoin.autotrading.record.entity.TradeEntity;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import com.setvect.bokslcoin.autotrading.record.repository.AssetHistoryRepository;
import com.setvect.bokslcoin.autotrading.record.repository.TradeRepository;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.LimitedSizeQueue;
import com.setvect.bokslcoin.autotrading.util.MathUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 이평선 돌파 전략 + 멀티 코인
 * 코인별 동일한 현금 비율로 매매를 수행한다.
 */
@Service("mabsMulti")
@RequiredArgsConstructor
@Slf4j
public class MabsMultiService implements CoinTrading {
    /**
     * 매수 시 즉각적인 매매를 위해 호가보다 상단에 주문을 넣는 퍼센트
     */
    private static final double DIFF_RATE_BUY = 0.0;
    /**
     * 매도 시 즉각적인 매매를 위해 호가보다 하단에 주문을 넣는 퍼센트
     * 무조건 매도를 하기위해 시장가 수준으로 호가를 낮춰 매도 요청
     */
    private static final double DIFF_RATE_SELL = 0.010;
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

    private final MabsMultiProperties properties;

    /**
     * 해당 기간에 매매 여부 완료 여부
     * value: 코인 예) KRW-BTC, KRW-ETH, ...
     */
    private final Set<String> tradeCompleteOfPeriod = new HashSet<>();

    private int periodIdx = -1;

    /**
     * (코인 코드: 최근 캔들 목록)
     */
    private final Map<String, LimitedSizeQueue<Candle>> coinByCandles = new HashMap<>();

    /**
     * 보유 자산
     * (코인코드: 계좌)
     */
    private final Map<String, Account> coinAccount = new HashMap<>();

    /**
     * 보유 자산
     * (코인코드: 매매 대기)
     */
    private final Map<String, OrderHistory> coinOrderWait = new HashMap<>();

    /**
     * 매수 이후 최고 수익률
     */
    private final Map<String, Double> highYield = new HashMap<>();
    /**
     * 매수 이후 최저 수익률
     */
    private final Map<String, Double> lowYield = new HashMap<>();

    /**
     * 정기적인 코인 시세 출력을 위한 마크
     */
    private final Set<String> logCoinMark = new HashSet<>();

    /**
     * 마지막 체결 시세
     * (코인코드: 체결)
     */
    private final Map<String, TradeResult> currentTradeResult = new HashMap<>();

    private int logRotation = -1;
    private int orderWaitRotation = -1;

    @Override
    public synchronized void tradeEvent(TradeResult tradeResult) {
        currentTradeResult.put(tradeResult.getCode(), tradeResult);

        if (coinByCandles.size() < properties.getMarkets().size()) {
            loadAccount();
            loadOrderWait();
            loadCandle();
        }

        LocalDateTime nowUtc = tradeResult.getTradeDateTimeUtc();
        int currentPeriod = getCurrentPeriod(nowUtc);


        // 새로운 날짜면 매매 다시 초기화
        if (periodIdx != currentPeriod) {
            tradeEvent.newPeriod(tradeResult);
            loadAccount();
            saveAsset(tradeResult.getTradeDateTimeKst());
            tradeCompleteOfPeriod.clear();
            periodIdx = currentPeriod;
        }

        String market = tradeResult.getCode();
        List<Candle> candles = coinByCandles.get(market);
        if (candles == null) {
            slackMessageService.sendMessage(String.format("%s 설정에 없는 시세데이타가 조회 되었습니다.", market));
            return;
        }

        // 맨 앞에 가장 최근
        Candle newestCandle = candles.get(0);
        LocalDateTime tradeDateTimeKst = properties.getPeriodType().fitDateTime(tradeResult.getTradeDateTimeKst());
        LocalDateTime candleDateTimeKst = properties.getPeriodType().fitDateTime(newestCandle.getCandleDateTimeKst());

        if (candleDateTimeKst.equals(tradeDateTimeKst)) {
            newestCandle.change(tradeResult);
        } else {
            newestCandle = new Candle(tradeResult);
            // 최근 캔들이 맨 앞에 있기 때문에 index 0에 넣음
            candles.add(0, newestCandle);
        }

        double maShort = CommonTradeHelper.getMa(candles, properties.getShortPeriod());
        double maLong = CommonTradeHelper.getMa(candles, properties.getLongPeriod());
        tradeEvent.check(newestCandle, maShort, maLong);

        logCurrentPrice(tradeResult, maShort, maLong);
        checkOrderWait();

        if (isBuyable(market)) {
            doBid(market);
        } else if (isSellable(market)) {
            doAsk(market);
        }
    }

    /**
     * @param market 매수 대상 코인
     * @return true 매수 조건 만족
     */
    private boolean isBuyable(String market) {
        // 해당 주기에서 매도한 종목은 다시 매수 하지 않음
        if (tradeCompleteOfPeriod.contains(market)) {
            return false;
        }
        if (coinAccount.containsKey(market)) {
            return false;
        }

        List<Candle> candleList = coinByCandles.get(market);
        double maShort = CommonTradeHelper.getMa(candleList, properties.getShortPeriod());
        double maLong = CommonTradeHelper.getMa(candleList, properties.getLongPeriod());

        double buyTargetPrice = maLong + maLong * properties.getUpBuyRate();

        //(장기이평 + 장기이평 * 상승매수률) <= 단기이평
        boolean isBuy = buyTargetPrice <= maShort;
        double cash = getBuyCash();
        if (!isBuy || !(cash >= MINIMUM_BUY_CASH)) {
            return false;
        }
        // 매수 직전 주문 요청 이력 확인
        loadOrderWait();
        OrderHistory orderHistory = coinOrderWait.get(market);
        if (orderHistory != null) {
            log.info("{} 매수 주문요청  상태임: {}", market, orderHistory);
            return false;
        }
        // 직전 이동평균을 감지해 새롭게 돌파 했을 때만 매수
        boolean isBeforeBuy = isBeforeBuy(candleList);
        Candle candle = candleList.get(0);
        if (isBeforeBuy && properties.isNewMasBuy()) {
            log.debug("[{}] 매수 안함. 새롭게 이동평균을 돌파할 때만 매수합니다.", candle.getMarket());
            return false;
        }
        // 매수 대기 처리 여부 확인을 위해 계좌 내역 한번더 조회
        loadAccount();
        Account account = coinAccount.get(market);
        if (account == null) {
            log.warn("[{}] 매수 안함. 이미 매수한 종목", candle.getMarket());
            return false;
        }
        return true;
    }

    /**
     * 코인 매수
     *
     * @param market 코인 종류
     */
    private void doBid(String market) {
        List<Candle> candleList = coinByCandles.get(market);
        Candle candle = candleList.get(0);
        double tradePrice = candle.getTradePrice();

        double bidPrice = getBuyCash();
        // 매수 가격, 높은 가격으로 매수(시장가 효과)
        double fitPrice = AskPriceRange.askPrice(tradePrice + tradePrice * DIFF_RATE_BUY);

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
        loadAccount();
        loadOrderWait();
    }


    /**
     * @param market 매도 대상 코인
     * @return true 매도 조건 만족
     */
    private boolean isSellable(String market) {
        Account account = coinAccount.get(market);
        if (account == null) {
            return false;
        }
        List<Candle> candleList = coinByCandles.get(market);

        Candle candle = candleList.get(0);
        double yield = getYield(candle, account);

        double maxHighYield = Math.max(highYield.getOrDefault(market, 0.0), yield);
        highYield.put(market, maxHighYield);
        tradeEvent.highYield(candle.getMarket(), maxHighYield);

        double minLowYield = Math.min(lowYield.getOrDefault(market, 0.0), yield);
        lowYield.put(market, minLowYield);
        tradeEvent.lowYield(candle.getMarket(), minLowYield);

        double maShort = CommonTradeHelper.getMa(candleList, properties.getShortPeriod());
        double maLong = CommonTradeHelper.getMa(candleList, properties.getLongPeriod());

        // 장기이평 >= (단기이평 + 단기이평 * 하락매도률)
        double sellTargetPrice = maShort + maShort * properties.getDownSellRate();
        boolean isSell = maLong >= sellTargetPrice;
        boolean isLossStop = properties.getLoseStopRate() < -yield;
        if (!(isSell || isLossStop)) {
            // 매도 하지 않음
            return false;
        }

        // 매도 직전 주문 요청 이력 확인
        loadOrderWait();
        OrderHistory orderHistory = coinOrderWait.get(market);
        if (orderHistory != null) {
            log.info("{} 매도 주문요청  상태임: {}", market, orderHistory);
            return false;
        }
        return true;

    }

    /**
     * 매도
     *
     * @param market 코인 종류
     */
    private void doAsk(String market) {
        Account account = coinAccount.get(market);
        List<Candle> candleList = coinByCandles.get(market);
        Candle candle = candleList.get(0);
        double yield = getYield(candle, account);

        String message = String.format("[%s] 현재가: %,.2f, 매입단가: %,.2f, 투자금: %,.0f, 수익률: %.2f%%, 최고 수익률: %.2f%%, 최저 수익률: %.2f%%",
                candle.getMarket(),
                candle.getTradePrice(),
                account.getAvgBuyPriceValue(),
                account.getInvestCash(),
                yield * 100,
                highYield.get(market) * 100,
                lowYield.get(market) * 100
        );
        log.info(message);
        slackMessageService.sendMessage(message);

        // 매도 가격, 낮은 가격으로 매도(시장가 효과)
        double currentPrice = candle.getTradePrice();
        double balance = account.getBalanceValue();
        double fitPrice = AskPriceRange.askPrice(currentPrice - currentPrice * DIFF_RATE_SELL);
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
        loadAccount();
        loadOrderWait();
    }

    /**
     * 5분마다 매매 시세 체크, 매매 대기가 있으면 슬랙으로 메시지 전달
     */
    private void checkOrderWait() {
        int temp = LocalTime.now().getMinute() / 5;
        if (orderWaitRotation == temp) {
            return;
        }
        orderWaitRotation = temp;

        loadOrderWait();
        String message = coinOrderWait.values().stream().map(history -> {
            String currentPrice = Optional.ofNullable(currentTradeResult.get(history.getMarket()))
                    .map(p -> ApplicationUtil.toNumberString(p.getTradePrice())).orElse("");
            return String.format("[%s] %s 대기 %s/%s, %s/%s",
                    removeKrw(history.getMarket()),
                    history.getSide().getName(),
                    history.getPrice(),
                    currentPrice,
                    history.getRemainingVolume(),
                    history.getVolume());
        }).collect(Collectors.joining("\n"));
        if (!StringUtils.isNotBlank(message)) {
            return;
        }
        log.info(message);
        slackMessageService.sendMessage(message);
    }

    /**
     * 10초마다 코인별 현재 가격 출력
     */
    private void logCurrentPrice(TradeResult tradeResult, double maShort, double maLong) {
        LocalTime now = LocalTime.now();
        int temp = now.getSecond() / 10;
        if (logRotation != temp) {
            logCoinMark.clear();
            logRotation = temp;
        }

        if (logCoinMark.contains(tradeResult.getCode())) {
            return;
        }
        logCoinMark.add(tradeResult.getCode());
        String message = String.format("[%s] 장-단: %,.2f(%.2f%%), %,.2f, MA_%d: %,.2f, MA_%d: %,.2f, TD: %,d",
                tradeResult.getCode(),
                maShort - maLong,
                MathUtil.getYield(maShort, maLong) * 100,
                tradeResult.getTradePrice(),
                properties.getShortPeriod(), maShort,
                properties.getLongPeriod(), maLong,
                tradeResult.getTimestampDiff()
        );
        log.debug(message);
    }

    /**
     * 자산 기록
     *
     * @param tradeDateTimeKst 현재 시간
     */
    private void saveAsset(LocalDateTime tradeDateTimeKst) {
        // 각 코인들의 가장 최근 캔들
        Map<String, Candle> lastCandle = coinByCandles.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, p -> p.getValue().get(0)));
        List<AssetHistoryEntity> rateByCoin = writeCurrentAssetRate(coinAccount, lastCandle, tradeDateTimeKst);

        sendCurrentStatus(rateByCoin);
    }

    /**
     * 현금, 매수 코인 목록
     */
    private void loadAccount() {
        coinAccount.clear();
        coinAccount.putAll(accountService.getMyAccountBalance());
        log.info("load account: {}", coinAccount);
    }

    /**
     * 매수/매도 대기 주문 조회
     */
    private void loadOrderWait() {
        List<OrderHistory> history = orderService.getHistory(0, properties.getMaxBuyCount());
        coinOrderWait.clear();
        coinOrderWait.putAll(history.stream().collect(Collectors.toMap(OrderHistory::getMarket, Function.identity())));
        log.info("load coinOrder: {}", coinOrderWait);
    }

    /**
     * 코인 캔들 정보를 얻음
     */
    private void loadCandle() {
        int candleMaxSize = properties.getLongPeriod() + 1;
        for (String market : properties.getMarkets()) {
            List<Candle> candleList = CommonTradeHelper.getCandles(candleService, market, properties.getPeriodType(), candleMaxSize);
            if (candleList.isEmpty()) {
                throw new RuntimeException(String.format("[%s] 현재 시세 데이터가 없습니다.", market));
            }
            if (candleList.size() < candleMaxSize) {
                throw new RuntimeException(String.format("[%s] 이동평균계산을 위한 시세 데이터가 부족합니다", market));
            }

            LimitedSizeQueue<Candle> candles = new LimitedSizeQueue<>(candleMaxSize);
            candles.addAll(candleList);
            coinByCandles.put(market, candles);
        }
        String candleInfo = coinByCandles.entrySet()
                .stream()
                .map(entity -> entity.getKey() + ": " + entity.getValue().size())
                .collect(Collectors.joining(", "));
        log.info("load candle: {}", candleInfo);
    }

    /**
     * @return 보유 현금
     */
    private double getCash() {
        Account krw = coinAccount.get("KRW");
        BigDecimal cash = BigDecimal.valueOf(krw.getBalanceValue());
        return cash.doubleValue();
    }

    /**
     * 전체 보유 현금, 최대 매수 건수, 현재 매수 코인를 기준으로 매수 금액을 계산
     *
     * @return 매수 금액
     */
    private double getBuyCash() {
        // 이미 매수한 코인 갯수
        int allowBuyCount = Math.min(properties.getMaxBuyCount(), properties.getMarkets().size());
        // 구매 건수 = 이미 구매 건수 + 매수 대기 건수 
        int buyCount = (int) properties.getMarkets().stream().filter(p -> coinAccount.containsKey(p) || coinOrderWait.containsKey(p)).count();
        int rate = allowBuyCount - buyCount;
        double buyCash = 0;

        if (rate > 0) {
            buyCash = (getCash() * properties.getInvestRatio()) / rate;
        }
        return buyCash;
    }

    /**
     * 현재 시세정보와 투자 수익률 슬렉으로 전달
     *
     * @param rateByCoin 코인 투자 수익률
     */
    private void sendCurrentStatus(List<AssetHistoryEntity> rateByCoin) {
        Map<String, AssetHistoryEntity> coinByAsset = rateByCoin.stream().collect(Collectors.toMap(AssetHistoryEntity::getCurrency, Function.identity()));

        String header = "종목, 장-단 차이, 1D수익, 현재가, 수익률";
        String priceMessage = coinByCandles.values().stream().map(
                candleList -> {
                    double maShort = CommonTradeHelper.getMa(candleList, properties.getShortPeriod());
                    double maLong = CommonTradeHelper.getMa(candleList, properties.getLongPeriod());
                    Candle candle = candleList.get(0);

                    String market = candle.getMarket();
                    TradeResult tradeResult = currentTradeResult.get(market);
                    String dayYield = Optional.ofNullable(tradeResult).map(p -> String.format("%.2f%%", p.getYieldDay() * 100)).orElse("X");
                    String message = String.format("[%s] %.2f%%, %s, %,.0f",
                            removeKrw(market),
                            MathUtil.getYield(maShort, maLong) * 100,
                            dayYield,
                            candle.getTradePrice()
                    );
                    AssetHistoryEntity asset = coinByAsset.get(market);
                    return message + Optional.ofNullable(asset).map(p -> String.format(", %.2f%%", p.getYield() * 100)).orElse(", _");
                }
        ).collect(Collectors.joining("\n"));

        double investment = rateByCoin.stream().filter(p -> !p.getCurrency().equals("KRW"))
                .mapToDouble(AssetHistoryEntity::getPrice).sum();
        double appraisal = rateByCoin.stream().filter(p -> !p.getCurrency().equals("KRW"))
                .mapToDouble(AssetHistoryEntity::getAppraisal).sum();
        double cash = rateByCoin.stream().filter(p -> p.getCurrency().equals("KRW"))
                .mapToDouble(AssetHistoryEntity::getPrice).sum();
        String investmentSummary = String.format("투자금: %,.0f, 평가금: %,.0f, 수익: %,.0f(%.2f%%)",
                investment, appraisal, appraisal - investment, ApplicationUtil.getYield(investment, appraisal) * 100);
        String cashSummary = String.format("보유현금: %,.0f, 합계 금액: %,.0f", cash, cash + appraisal);

        long maxDiff = currentTradeResult.values().stream().mapToLong(TradeResult::getTimestampDiff).max().orElse(-9999);
        long minDiff = currentTradeResult.values().stream().mapToLong(TradeResult::getTimestampDiff).min().orElse(-9999);
        String diffTimeSummary = String.format("시간차: 최대 %,d, 최소 %,d", maxDiff, minDiff);

        slackMessageService.sendMessage(StringUtils.joinWith("\n-----------\n",
                header, priceMessage, investmentSummary, cashSummary, diffTimeSummary));
    }


    /**
     * 현재 보유중인 코인 및 현금 수익률 저장
     *
     * @param accounts   Key: 계좌이름,Value: 계좌 정보
     * @param lastCandle 마지막 캔들
     * @param regDate    등록일
     * @return 자산별 수익률 정보
     */
    private List<AssetHistoryEntity> writeCurrentAssetRate
    (Map<String, Account> accounts, Map<String, Candle> lastCandle, LocalDateTime regDate) {
        List<AssetHistoryEntity> accountHistoryList = accounts.entrySet().stream().map(entity -> {
            Account account = entity.getValue();
            AssetHistoryEntity assetHistory = new AssetHistoryEntity();
            assetHistory.setCurrency(entity.getKey());
            double price = entity.getKey().equals("KRW") ? account.getBalanceValue() : account.getInvestCash();
            assetHistory.setPrice(price);

            Candle candle = lastCandle.get(entity.getKey());
            if (candle != null) {
                assetHistory.setYield(getYield(candle, account));
            } else {
                assetHistory.setYield(0.0);
            }
            assetHistory.setRegDate(regDate);
            return assetHistory;
        }).collect(Collectors.toList());

        assetHistoryRepository.saveAll(accountHistoryList);
        return accountHistoryList;
    }

    private int getCurrentPeriod(LocalDateTime nowUtc) {
        int dayHourMinuteSum = nowUtc.getDayOfMonth() * 1440 + nowUtc.getHour() * 60 + nowUtc.getMinute();
        return dayHourMinuteSum / properties.getPeriodType().getDiffMinutes();
    }

    /**
     * @param candleList 캔들
     * @return 이동 평균에서 직전 매수 조건 이면 true, 아니면 false
     */
    private boolean isBeforeBuy(List<Candle> candleList) {
        // 한단계전에 매수 조건이였는지 확인
        List<Candle> beforeCandleList = candleList.subList(1, candleList.size());
        double maShortBefore = CommonTradeHelper.getMa(beforeCandleList, properties.getShortPeriod());
        double maLongBefore = CommonTradeHelper.getMa(beforeCandleList, properties.getLongPeriod());
        double buyTargetPrice = maLongBefore + maLongBefore * properties.getUpBuyRate();
        //(장기이평 + 장기이평 * 상승매수률) <= 단기이평
        return buyTargetPrice <= maShortBefore;
    }


    private double getYield(Candle candle, Account account) {
        double avgPrice = account.getAvgBuyPriceValue();
        double diff = candle.getTradePrice() - avgPrice;
        return diff / avgPrice;
    }

    @Nullable
    private static String removeKrw(String market) {
        return StringUtils.replace(market, "KRW-", "");
    }
}
