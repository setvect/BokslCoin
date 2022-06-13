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
     * �ż� �� �ﰢ���� �ŸŸ� ���� ȣ������ ��ܿ� �ֹ��� �ִ� �ۼ�Ʈ
     */
    private static final double DIFF_RATE_BUY = 0.0;
    /**
     * �ŵ� �� �ﰢ���� �ŸŸ� ���� ȣ������ �ϴܿ� �ֹ��� �ִ� �ۼ�Ʈ
     * ������ �ŵ��� �ϱ����� ���尡 �������� ȣ���� ���� �ŵ� ��û
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
     * ���� �ڻ�
     * (�����ڵ�: ����)
     */
    private final Map<String, Account> coinAccount = new HashMap<>();
    /**
     * ���� �ڻ�
     * (�����ڵ�: �Ÿ� ���)
     */
    private final Map<String, OrderHistory> coinOrderWait = new HashMap<>();


    /**
     * (���� �ڵ�: �ֱ� ĵ�� ���)
     */
    private final Map<String, LimitedSizeQueue<Candle>> coinByCandles = new HashMap<>();
    /**
     * �ż� ���� �ְ� ���ͷ�
     */
    private final Map<String, Double> highYield = new HashMap<>();
    /**
     * �ż� ���� ���� ���ͷ�
     */
    private final Map<String, Double> lowYield = new HashMap<>();
    /**
     * �ش� �Ⱓ�� �Ÿ� ���� �Ϸ� ����
     * value: ���� ��) KRW-BTC, KRW-ETH, ...
     */
    private final Set<String> tradeCompleteOfPeriod = new HashSet<>();

    private int orderWaitRotation = -1;

    /**
     * �Ÿ��� ���� ���� �Ӽ���
     */
    // TODO ���� ������ ���������� ����� �� ������ �����. ��? spring instance�� �⺻������ �̱����̴ϱ�.
    private TradeCommonParameter parameter;

    /**
     * ���� ���۽� ���� ���� �ʱ�ȭ
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
     * ����, �ż� ���� ���
     */
    public void loadAccount() {
        coinAccount.clear();
        coinAccount.putAll(accountService.getMyAccountBalance());
        log.debug("load account: {}", coinAccount);
    }

    /**
     * �ż�/�ŵ� ��� �ֹ� ��ȸ
     */
    // TODO ������
    public void loadOrderWait() {
        List<OrderHistory> history = orderService.getHistory(0, parameter.getMaxBuyCount());
        coinOrderWait.clear();
        coinOrderWait.putAll(history.stream().collect(Collectors.toMap(OrderHistory::getMarket, Function.identity())));
        log.debug("load coinOrder: {}", coinOrderWait);
    }

    /**
     * ���� ĵ�� ������ ����
     */
    public void loadCandle() {
        for (String market : parameter.getMarkets()) {
            List<Candle> candleList = CommonTradeHelper.getCandles(candleService, market, parameter.getPeriodType(), parameter.getCandleLoadCount());
            if (candleList.isEmpty()) {
                throw new RuntimeException(String.format("[%s] ���� �ü� �����Ͱ� �����ϴ�.", market));
            }
            if (candleList.size() < parameter.getCandleLoadCount()) {
                throw new RuntimeException(String.format("[%s] �̵���հ���� ���� �ü� �����Ͱ� �����մϴ�", market));
            }

            LimitedSizeQueue<Candle> candles = new LimitedSizeQueue<>(parameter.getCandleLoadCount());
            candles.addAll(candleList);
            coinByCandles.put(market, candles);
        }
        String candleInfo = coinByCandles.entrySet().stream().map(entity -> entity.getKey() + ": " + entity.getValue().size()).collect(Collectors.joining(", "));
        log.info("load candle: {}", candleInfo);
    }

    /**
     * ���� �ż�
     *
     * @param market ���� ����
     */
    public void doBid(String market) {
        List<Candle> candleList = getCandles(market);
        Candle candle = candleList.get(0);
        double tradePrice = candle.getTradePrice();

        double bidPrice = getBuyCash();
        // �ż� ����, ���� �������� �ż�(���尡 ȿ��)
        double fitPrice = AskPriceRange.askPrice(tradePrice + tradePrice * DIFF_RATE_BUY);

        // �ż� ����
        String volume = ApplicationUtil.toNumberString(bidPrice / fitPrice);

        String price = ApplicationUtil.toNumberString(fitPrice);
        orderService.callOrderBid(market, volume, price);

        TradeEntity trade = new TradeEntity();
        trade.setMarket(market);
        trade.setTradeType(TradeType.BUY);
        // �ż� ȣ�� ���� ���� ü�� �� ���ɼ��� �ֱ� ������ ������ ����
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
     * �ŵ�
     *
     * @param market ���� ����
     */
    public void doAsk(String market) {
        Account account = getAccount(market);
        List<Candle> candleList = getCandles(market);
        Candle candle = candleList.get(0);
        double yield = TradeCommonUtil.getYield(candle, account);

        String message = String.format("[%s] ���簡: %,.2f, ���Դܰ�: %,.2f, ���ڱ�: %,.0f, ���ͷ�: %.2f%%, �ְ� ���ͷ�: %.2f%%, ���� ���ͷ�: %.2f%%",
                candle.getMarket(),
                candle.getTradePrice(),
                account.getAvgBuyPriceValue(),
                account.getInvestCash(),
                yield * 100,
                highYield.get(market) * 100,
                lowYield.get(market) * 100);

        log.info(message);
        slackMessageService.sendMessage(message);

        // �ŵ� ����, ���� �������� �ŵ�(���尡 ȿ��)
        double currentPrice = candle.getTradePrice();
        double balance = account.getBalanceValue();
        double fitPrice = AskPriceRange.askPrice(currentPrice - currentPrice * DIFF_RATE_SELL);
        orderService.callOrderAsk(market, ApplicationUtil.toNumberString(balance), ApplicationUtil.toNumberString(fitPrice));

        TradeEntity trade = new TradeEntity();
        trade.setMarket(market);
        trade.setTradeType(TradeType.SELL);
        // �ŵ� ȣ�� ���� ���� ü�� �� ���ɼ��� �ֱ� ������ ������ ����
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
     * ���� ���� �ְ� ������ �̺�Ʈ �ݿ�
     *
     * @param market �����ڵ�
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
     * �ڻ� ���
     *
     * @param tradeDateTimeKst ���� �ð�
     * @return �ڻ꺰 ���ͷ� ����
     */
    public List<AssetHistoryEntity> saveAsset(LocalDateTime tradeDateTimeKst) {
        // �� ���ε��� ���� �ֱ� ĵ��
        Map<String, Candle> lastCandle = coinByCandles.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, p -> p.getValue().get(0)));
        return writeCurrentAssetRate(coinAccount, lastCandle, tradeDateTimeKst);
    }


    /**
     * 5�и��� �Ÿ� �ü� üũ, �Ÿ� ��Ⱑ ������ �������� �޽��� ����
     * �Ÿ� ��Ⱑ ������ ���� ������ �߰��� üũ��
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
            return String.format("[%s] %s ��� %s/%s, %s/%s", TradeCommonUtil.removeKrw(history.getMarket()), history.getSide().getName(), history.getPrice(), currentPrice, history.getRemainingVolume(), history.getVolume());
        }).collect(Collectors.joining("\n"));
        if (!StringUtils.isNotBlank(message)) {
            return;
        }
        loadAccount();
        log.info(message);
        slackMessageService.sendMessage(message);
    }

    /**
     * ��ü ���� ����, �ִ� �ż� �Ǽ�, ���� �ż� ���θ� �������� �ż� �ݾ��� ���
     *
     * @return �ż� �ݾ�
     */
    public double getBuyCash() {
        // �̹� �ż��� ���� ����
        int allowBuyCount = Math.min(parameter.getMaxBuyCount(), parameter.getMarkets().size());
        // ���� �Ǽ� = �̹� ���� �Ǽ� + �ż� ��� �Ǽ�
        int buyCount = (int) parameter.getMarkets().stream().filter(p -> coinAccount.containsKey(p) || coinOrderWait.containsKey(p)).count();
        int rate = allowBuyCount - buyCount;
        double buyCash = 0;

        if (rate > 0) {
            buyCash = (getCash() * parameter.getInvestRatio()) / rate;
        }
        return buyCash;
    }

    /**
     * @return ���� ����
     */
    private double getCash() {
        Account krw = coinAccount.get("KRW");
        BigDecimal cash = BigDecimal.valueOf(krw.getBalanceValue());
        return cash.doubleValue();
    }

    /**
     * ���� �������� ���� �� ���� ���ͷ� ����
     *
     * @param accounts   Key: �����̸�,Value: ���� ����
     * @param lastCandle ������ ĵ��
     * @param regDate    �����
     * @return �ڻ꺰 ���ͷ� ����
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
     * ���� �ü������� ���� ���ͷ� �������� ����
     *
     * @param rateByCoin         ���� ���� ���ͷ�
     * @param currentTradeResult ������ ü�� �ü� (�����ڵ�: ü��)
     * @param tradeInfoMessage   �Ÿ� ��� �ü� ���� ����
     */
    public void sendCurrentStatus(List<AssetHistoryEntity> rateByCoin, Map<String, TradeResult> currentTradeResult, String tradeInfoMessage) {
        double investment = rateByCoin.stream().filter(p -> !p.getCurrency().equals("KRW")).mapToDouble(AssetHistoryEntity::getPrice).sum();
        double appraisal = rateByCoin.stream().filter(p -> !p.getCurrency().equals("KRW")).mapToDouble(AssetHistoryEntity::getAppraisal).sum();
        double cash = rateByCoin.stream().filter(p -> p.getCurrency().equals("KRW")).mapToDouble(AssetHistoryEntity::getPrice).sum();
        String investmentSummary = String.format("���ڱ�: %,.0f, �򰡱�: %,.0f, ����: %,.0f(%.2f%%)",
                investment, appraisal, appraisal - investment, ApplicationUtil.getYield(investment, appraisal) * 100);
        String cashSummary = String.format("��������: %,.0f, �հ� �ݾ�: %,.0f", cash, cash + appraisal);

        long maxDiff = currentTradeResult.values().stream().mapToLong(TradeResult::getTimestampDiff).max().orElse(-9999);
        long minDiff = currentTradeResult.values().stream().mapToLong(TradeResult::getTimestampDiff).min().orElse(-9999);
        String diffTimeSummary = String.format("�ð���: �ִ� %,d, �ּ� %,d", maxDiff, minDiff);

        slackMessageService.sendMessage(StringUtils.joinWith("\n-----------\n",
                tradeInfoMessage, investmentSummary, cashSummary, diffTimeSummary));
    }

    /**
     * @param market ����
     * @return ���� ���� ���� ����
     */
    public Account getAccount(String market) {
        return coinAccount.get(market);
    }

}
