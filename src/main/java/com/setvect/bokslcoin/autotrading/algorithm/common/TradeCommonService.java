package com.setvect.bokslcoin.autotrading.algorithm.common;

import com.setvect.bokslcoin.autotrading.algorithm.AskPriceRange;
import com.setvect.bokslcoin.autotrading.algorithm.AskReason;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeCommonService {
    /**
     * 매수 시 즉각적인 매매를 위해 호가보다 상단에 주문을 넣는 퍼센트
     */
    private static final double DIFF_RATE_BUY = 0.0;
    /**
     * 매도 시 즉각적인 매매를 위해 호가보다 하단에 주문을 넣는 퍼센트
     * 무조건 매도를 하기위해 시장가 수준으로 호가를 낮춰 매도 요청
     */
    private static final double DIFF_RATE_SELL = 0.010;

    private final TradeEvent tradeEvent;
    private final AccountService accountService;
    private final OrderService orderService;
    private final CandleService candleService;
    private final TradeRepository tradeRepository;
    private final SlackMessageService slackMessageService;
    private final AssetHistoryRepository assetHistoryRepository;


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
     * (코인 코드: 최근 캔들 목록)
     */
    private final Map<String, LimitedSizeQueue<Candle>> coinByCandles = new HashMap<>();
    /**
     * 매수 이후 최고 수익률
     */
    private final Map<String, Double> highYield = new HashMap<>();
    /**
     * 매수 이후 최저 수익률
     */
    private final Map<String, Double> lowYield = new HashMap<>();
    /**
     * 해당 기간에 매매 여부 완료 여부
     * value: 코인 예) KRW-BTC, KRW-ETH, ...
     */
    private final Set<String> tradeCompleteOfPeriod = new HashSet<>();

    private int orderWaitRotation = -1;

    /**
     * 매매을 위한 각종 속성값
     */
    // TODO 여러 전략을 복합적으로 사용할 때 문제가 예상됨. 왜? spring instance는 기본적으로 싱글톤이니깐.
    private TradeCommonParameter parameter;

    /**
     * 최초 시작시 각종 정보 초기화
     */
    public void init(TradeCommonParameter parameter) {
        this.parameter = parameter;
        loadAccount();
        loadOrderWait();
        loadCandle();
    }

    public int getCoinCandleSize() {
        return coinByCandles.size();
    }

    public LimitedSizeQueue<Candle> getCandles(String market) {
        return coinByCandles.get(market);
    }


    public Map<String, LimitedSizeQueue<Candle>> getCoinByCandles() {
        return Collections.unmodifiableMap(coinByCandles);
    }

    public boolean existCoin(String market) {
        return coinAccount.containsKey(market);
    }

    public OrderHistory getOrderHistory(String market) {
        return coinOrderWait.get(market);
    }

    public boolean existTradeCompleteOfPeriod(String market) {
        return tradeCompleteOfPeriod.contains(market);
    }

    public void clearTradeCompleteOfPeriod() {
        tradeCompleteOfPeriod.clear();
    }

    /**
     * 현금, 매수 코인 목록
     */
    public void loadAccount() {
        coinAccount.clear();
        coinAccount.putAll(accountService.getMyAccountBalance());
        log.debug("load account: {}", coinAccount);
    }

    /**
     * 매수/매도 대기 주문 조회
     */
    // TODO 공통모듈
    public void loadOrderWait() {
        List<OrderHistory> history = orderService.getHistory(0, parameter.getMaxBuyCount());
        coinOrderWait.clear();
        coinOrderWait.putAll(history.stream().collect(Collectors.toMap(OrderHistory::getMarket, Function.identity())));
        log.debug("load coinOrder: {}", coinOrderWait);
    }

    /**
     * 코인 캔들 정보를 얻음
     */
    public void loadCandle() {
        for (String market : parameter.getMarkets()) {
            List<Candle> candleList = CommonTradeHelper.getCandles(candleService, market, parameter.getPeriodType(), parameter.getCandleLoadCount());
            if (candleList.isEmpty()) {
                throw new RuntimeException(String.format("[%s] 현재 시세 데이터가 없습니다.", market));
            }
            if (candleList.size() < parameter.getCandleLoadCount()) {
                throw new RuntimeException(String.format("[%s] 이동평균계산을 위한 시세 데이터가 부족합니다", market));
            }

            LimitedSizeQueue<Candle> candles = new LimitedSizeQueue<>(parameter.getCandleLoadCount());
            candles.addAll(candleList);
            coinByCandles.put(market, candles);
        }
        String candleInfo = coinByCandles.entrySet().stream().map(entity -> entity.getKey() + ": " + entity.getValue().size()).collect(Collectors.joining(", "));
        log.info("load candle: {}", candleInfo);
    }

    /**
     * 코인 매수
     *
     * @param market 코인 종류
     */
    public void doBid(String market) {
        List<Candle> candleList = getCandles(market);
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
     * 매도
     *
     * @param market 코인 종류
     */
    public void doAsk(String market) {
        Account account = getAccount(market);
        List<Candle> candleList = getCandles(market);
        Candle candle = candleList.get(0);
        double yield = TradeCommonUtil.getYield(candle, account);

        String message = String.format("[%s] 현재가: %,.2f, 매입단가: %,.2f, 투자금: %,.0f, 수익률: %.2f%%, 최고 수익률: %.2f%%, 최저 수익률: %.2f%%",
                candle.getMarket(),
                candle.getTradePrice(),
                account.getAvgBuyPriceValue(),
                account.getInvestCash(),
                yield * 100,
                highYield.get(market) * 100,
                lowYield.get(market) * 100);

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
     * 보유 종목 최고가 최저가 이벤트 반영
     *
     * @param market 종목코드
     */
    public void emitHighMinYield(String market) {
        Account account = getAccount(market);
        List<Candle> candleList = getCandles(market);

        Candle candle = candleList.get(0);
        double yield = TradeCommonUtil.getYield(candle, account);

        double maxHighYield = Math.max(highYield.getOrDefault(market, 0.0), yield);
        highYield.put(market, maxHighYield);
        tradeEvent.highYield(candle.getMarket(), maxHighYield);

        double minLowYield = Math.min(lowYield.getOrDefault(market, 0.0), yield);
        lowYield.put(market, minLowYield);
        tradeEvent.lowYield(candle.getMarket(), minLowYield);

    }

    /**
     * 자산 기록
     *
     * @param tradeDateTimeKst 현재 시간
     * @return 자산별 수익률 정보
     */
    public List<AssetHistoryEntity> saveAsset(LocalDateTime tradeDateTimeKst) {
        // 각 코인들의 가장 최근 캔들
        Map<String, Candle> lastCandle = coinByCandles.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, p -> p.getValue().get(0)));
        return writeCurrentAssetRate(coinAccount, lastCandle, tradeDateTimeKst);
    }


    /**
     * 5분마다 매매 시세 체크, 매매 대기가 있으면 슬랙으로 메시지 전달
     * 매매 대기가 있으면 계좌 정보도 추가로 체크함
     */
    public void checkStatus(Map<String, TradeResult> currentTradeResult) {
        int temp = LocalTime.now().getMinute() / 5;
        if (orderWaitRotation == temp) {
            return;
        }
        orderWaitRotation = temp;

        loadOrderWait();
        String message = coinOrderWait.values().stream().map(history -> {
            String currentPrice = Optional.ofNullable(currentTradeResult.get(history.getMarket())).map(p -> ApplicationUtil.toNumberString(p.getTradePrice())).orElse("");
            return String.format("[%s] %s 대기 %s/%s, %s/%s", TradeCommonUtil.removeKrw(history.getMarket()), history.getSide().getName(), history.getPrice(), currentPrice, history.getRemainingVolume(), history.getVolume());
        }).collect(Collectors.joining("\n"));
        if (!StringUtils.isNotBlank(message)) {
            return;
        }
        loadAccount();
        log.info(message);
        slackMessageService.sendMessage(message);
    }

    /**
     * 전체 보유 현금, 최대 매수 건수, 현재 매수 코인를 기준으로 매수 금액을 계산
     *
     * @return 매수 금액
     */
    public double getBuyCash() {
        // 이미 매수한 코인 갯수
        int allowBuyCount = Math.min(parameter.getMaxBuyCount(), parameter.getMarkets().size());
        // 구매 건수 = 이미 구매 건수 + 매수 대기 건수
        int buyCount = (int) parameter.getMarkets().stream().filter(p -> coinAccount.containsKey(p) || coinOrderWait.containsKey(p)).count();
        int rate = allowBuyCount - buyCount;
        double buyCash = 0;

        if (rate > 0) {
            buyCash = (getCash() * parameter.getInvestRatio()) / rate;
        }
        return buyCash;
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
            double price = entity.getKey().equals("KRW") ? account.getBalanceValueWithLock() : account.getInvestCashWithLock();
            assetHistory.setPrice(price);

            Candle candle = lastCandle.get(entity.getKey());
            if (candle != null) {
                assetHistory.setYield(TradeCommonUtil.getYield(candle, account));
            } else {
                assetHistory.setYield(0.0);
            }
            assetHistory.setRegDate(regDate);
            return assetHistory;
        }).collect(Collectors.toList());
        assetHistoryRepository.saveAll(accountHistoryList);
        return accountHistoryList;
    }

    /**
     * 현재 시세정보와 투자 수익률 슬렉으로 전달
     *
     * @param rateByCoin         코인 투자 수익률
     * @param currentTradeResult 마지막 체결 시세 (코인코드: 체결)
     * @param tradeInfoMessage   매매 대상 시세 코인 정보
     */
    public void sendCurrentStatus(List<AssetHistoryEntity> rateByCoin, Map<String, TradeResult> currentTradeResult, String tradeInfoMessage) {
        double investment = rateByCoin.stream().filter(p -> !p.getCurrency().equals("KRW")).mapToDouble(AssetHistoryEntity::getPrice).sum();
        double appraisal = rateByCoin.stream().filter(p -> !p.getCurrency().equals("KRW")).mapToDouble(AssetHistoryEntity::getAppraisal).sum();
        double cash = rateByCoin.stream().filter(p -> p.getCurrency().equals("KRW")).mapToDouble(AssetHistoryEntity::getPrice).sum();
        String investmentSummary = String.format("투자금: %,.0f, 평가금: %,.0f, 수익: %,.0f(%.2f%%)",
                investment, appraisal, appraisal - investment, ApplicationUtil.getYield(investment, appraisal) * 100);
        String cashSummary = String.format("보유현금: %,.0f, 합계 금액: %,.0f", cash, cash + appraisal);

        long maxDiff = currentTradeResult.values().stream().mapToLong(TradeResult::getTimestampDiff).max().orElse(-9999);
        long minDiff = currentTradeResult.values().stream().mapToLong(TradeResult::getTimestampDiff).min().orElse(-9999);
        String diffTimeSummary = String.format("시간차: 최대 %,d, 최소 %,d", maxDiff, minDiff);

        slackMessageService.sendMessage(StringUtils.joinWith("\n-----------\n",
                tradeInfoMessage, investmentSummary, cashSummary, diffTimeSummary));
    }

    /**
     * @param market 종목
     * @return 종목에 대한 계좌 정보
     */
    public Account getAccount(String market) {
        return coinAccount.get(market);
    }

}
