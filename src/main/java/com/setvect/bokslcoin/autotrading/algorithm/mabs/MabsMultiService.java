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
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import com.setvect.bokslcoin.autotrading.record.entity.AssetHistoryEntity;
import com.setvect.bokslcoin.autotrading.record.entity.TradeEntity;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import com.setvect.bokslcoin.autotrading.record.repository.AssetHistoryRepository;
import com.setvect.bokslcoin.autotrading.record.repository.TradeRepository;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.MathUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final Map<String, CircularFifoQueue<Candle>> coinByCandles = new HashMap<>();

    /**
     * 보유 자산
     * (코인코드: 코인정보)
     * TODO '자산' 이름 변경
     * TODO 매수/ 매도 사이에 딜레이를 생각해서 구현해야됨.
     */
    private Map<String, Account> coinAccount;

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
        // 사용하기
    }

    @Override
    public synchronized void tradeEvent(TradeResult tradeResult) {
        if (coinByCandles.isEmpty()) {
            loadStatus();
        }

        LocalDateTime nowUtc = tradeResult.getTradeDateTimeUtc();
        int currentPeriod = getCurrentPeriod(nowUtc);

        // 새로운 날짜면 매매 다시 초기화
        if (periodIdx != currentPeriod) {
            tradeEvent.newPeriod(tradeResult);
            periodIdx = currentPeriod;
            tradeCompleteOfPeriod.clear();
            saveAsset();
        }

        String market = tradeResult.getCode();
        CircularFifoQueue<Candle> candles = coinByCandles.get(market);
        if (candles == null) {
            slackMessageService.sendMessage(String.format("%s 설정에 없는 시세데이타가 조회 되었습니다.", market));
            return;
        }

        Candle candle = candles.get(candles.size() - 1);
        LocalDateTime tradeDateTimeKst = properties.getPeriodType().fitDateTime(tradeResult.getTradeDateTimeKst());
        if (candle.getCandleDateTimeKst().equals(tradeDateTimeKst)) {
            candle.change(tradeResult);
        } else {
            candles.add(new Candle(tradeResult, properties.getPeriodType()));
        }

        ArrayList<Candle> candleList = new ArrayList<>(candles);
        String checkMessage = checkMa(candleList);
        // TODO 메시지 보내기
//        priceCheckMessageList.add(checkMessage);
        coinAccount.get(market);

        Account account = coinAccount.get(market);
        if (!tradeCompleteOfPeriod.contains(market) && getCash() != 0) {
            buyCheck(getBuyCash(), candleList);
        } else if (account != null) {
            sellCheck(account, candleList);
        }
    }

    /**
     * 자산 기록
     * TODO 크론텝 형식으로 변경
     */
    private void saveAsset() {
        List<Candle> candles = properties.getMarkets().stream().map(
                market -> CommonTradeHelper.getCandles(candleService, market, properties.getPeriodType(), 1).get(0)
        ).collect(Collectors.toList());
        Map<String, Candle> lastCandle = candles.stream().collect(Collectors.toMap(Candle::getMarket, Function.identity()));

        List<AssetHistoryEntity> rateByCoin = writeCurrentAssetRate(coinAccount, lastCandle, candles.get(0).getCandleDateTimeKst());

//        sendCurrentStatus(priceCheckMessageList, rateByCoin);
    }

    /**
     * 현금, 매수 코인 목록, 코인 캔들 정보를 얻음
     */
    private void loadStatus() {
        coinAccount = accountService.getMyAccountBalance();
        int candleMaxSize = properties.getLongPeriod() + 1;
        for (String market : properties.getMarkets()) {
            List<Candle> candleList = CommonTradeHelper.getCandles(candleService, market, properties.getPeriodType(), candleMaxSize);
            if (candleList.isEmpty()) {
                throw new RuntimeException(String.format("[%s] 현재 시세 데이터가 없습니다.", market));
            }
            if (candleList.size() < candleMaxSize) {
                throw new RuntimeException(String.format("[%s] 이동평균계산을 위한 시세 데이터가 부족합니다", market));
            }

            CircularFifoQueue<Candle> candles = new CircularFifoQueue<>(candleMaxSize);
            candles.addAll(candleList);
            coinByCandles.put(market, candles);
        }
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
        int buyCount = (int) properties.getMarkets().stream().filter(p -> coinAccount.get(p) != null).count();
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
     * @param currentPriceMessage 코인별 시세 정보
     * @param rateByCoin          코인 투자 수익률
     */
    private void sendCurrentStatus(List<String> currentPriceMessage, List<AssetHistoryEntity> rateByCoin) {
        String priceMessage = StringUtils.join(currentPriceMessage, "\n");
        String rateMessage = rateByCoin.stream()
                .filter(p -> !p.getCurrency().equals("KRW"))
                .map(p -> String.format("[%s] 수익률: %,.2f%%", p.getCurrency(), p.getYield() * 100))
                .collect(Collectors.joining("\n"));

        slackMessageService.sendMessage(StringUtils.joinWith("\n-----------\n", priceMessage, rateMessage));
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
            }
            assetHistory.setRegDate(regDate);
            return assetHistory;
        }).collect(Collectors.toList());

        assetHistoryRepository.saveAll(accountHistoryList);
        return accountHistoryList;
    }

    /**
     * 시세 체크
     * // TODO 사용하기
     *
     * @param candleList 캔들 값
     * @return 시세 체크
     */
    private String checkMa(List<Candle> candleList) {
        double maShort = CommonTradeHelper.getMa(candleList, properties.getShortPeriod());
        double maLong = CommonTradeHelper.getMa(candleList, properties.getLongPeriod());
        Candle candle = candleList.get(0);
        tradeEvent.check(candle, maShort, maLong);
        String message = String.format("[%s] 단기-장기 차이: %,.2f(%.2f%%), 현재가: %,.2f, MA_%d: %,.2f, MA_%d: %,.2f",
                candle.getMarket(),
                maShort - maLong,
                MathUtil.getYield(maShort, maLong) * 100,
                candle.getTradePrice(),
                properties.getShortPeriod(), maShort,
                properties.getLongPeriod(), maLong
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
        double maShort = CommonTradeHelper.getMa(candleList, properties.getShortPeriod());
        double maLong = CommonTradeHelper.getMa(candleList, properties.getLongPeriod());

        Candle candle = candleList.get(0);
        double buyTargetPrice = maLong + maLong * properties.getUpBuyRate();
        String market = candle.getMarket();

        //(장기이평 + 장기이평 * 상승매수률) <= 단기이평
        boolean isBuy = buyTargetPrice <= maShort;

        if (isBuy && cash >= MINIMUM_BUY_CASH) {
            // 직전 이동평균을 감지해 새롭게 돌파 했을 때만 매수
            boolean isBeforeBuy = isBeforeBuy(candleList);
            if (isBeforeBuy && properties.isNewMasBuy()) {
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

        double maShort = CommonTradeHelper.getMa(candleList, properties.getShortPeriod());
        double maLong = CommonTradeHelper.getMa(candleList, properties.getLongPeriod());

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
        double sellTargetPrice = maShort + maShort * properties.getDownSellRate();
        boolean isSell = maLong >= sellTargetPrice;

        if (isSell || properties.getLoseStopRate() < -rate) {
            slackMessageService.sendMessage(message1);
            doAsk(market, candle.getTradePrice(), account.getBalanceValue(), rate);
        }
    }

    private int getCurrentPeriod(LocalDateTime nowUtc) {
        int dayHourMinuteSum = nowUtc.getDayOfMonth() * 1440 + nowUtc.getHour() * 60 + nowUtc.getMinute();
        return dayHourMinuteSum / properties.getPeriodType().getDiffMinutes();
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
}
