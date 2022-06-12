package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.mock;

import com.setvect.bokslcoin.autotrading.algorithm.AskReason;
import com.setvect.bokslcoin.autotrading.algorithm.BasicTradeEvent;
import com.setvect.bokslcoin.autotrading.backtest.common.BacktestHelper;
import com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.MabsMultiBacktestRow;
import com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.MabsTradeAnalyzerTest;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;


public class MockTradeEvent extends BasicTradeEvent {
    @Setter
    private Map<String, MabsTradeAnalyzerTest.CurrentPrice> priceMap;
    @Setter
    private Map<String, Account> accountMap;
    @Setter
    private List<MabsMultiBacktestRow> tradeHistory;
    /**
     * 최고 수익률
     */
    @Getter
    private double highYield;
    /**
     * 최저 수익률
     */
    @Getter
    private double lowYield;

    public MockTradeEvent(SlackMessageService slackMessageService) {
        super(slackMessageService);
    }

    @Override
    public void check(Candle candle, double maShort, double maLong) {
        priceMap.put(candle.getMarket(), new MabsTradeAnalyzerTest.CurrentPrice(candle, maShort, maLong));
    }

    @Override
    public void bid(String market, double tradePrice, double bidPrice) {
        MabsTradeAnalyzerTest.CurrentPrice currentPrice = priceMap.get(market);
        Candle candle = currentPrice.getCandle();
        MabsMultiBacktestRow backtestRow = new MabsMultiBacktestRow(BacktestHelper.depthCopy(candle));

        Account coinAccount = accountMap.get(market);
        coinAccount.setAvgBuyPrice(ApplicationUtil.toNumberString(tradePrice));
        double investAmount = bidPrice;

        Account krwAccount = accountMap.get("KRW");
        double cash = Double.parseDouble(krwAccount.getBalance()) - investAmount;
        krwAccount.setBalance(ApplicationUtil.toNumberString(cash));

        String balance = ApplicationUtil.toNumberString(investAmount / tradePrice);
        coinAccount.setBalance(balance);

        backtestRow.setTradeEvent(TradeType.BUY);
        backtestRow.setBidPrice(tradePrice);
        backtestRow.setBuyAmount(investAmount);
        backtestRow.setBuyTotalAmount(BacktestHelper.getBuyTotalAmount(accountMap));
        backtestRow.setCash(cash);
        backtestRow.setMaShort(currentPrice.getMaShort());
        backtestRow.setMaLong(currentPrice.getMaLong());

        tradeHistory.add(backtestRow);
    }

    @Override
    public void ask(String market, double balance, double tradePrice, AskReason reason) {
        MabsTradeAnalyzerTest.CurrentPrice currentPrice = priceMap.get(market);
        Candle candle = currentPrice.getCandle();

        MabsMultiBacktestRow backtestRow = new MabsMultiBacktestRow(BacktestHelper.depthCopy(candle));

        Account coinAccount = accountMap.get(market);
        backtestRow.setBidPrice(coinAccount.getAvgBuyPriceValue());
        backtestRow.setBuyAmount(coinAccount.getInvestCash());

//        double balance = Double.parseDouble(coinAccount.getBalance());
        double askAmount = tradePrice * balance;

        Account krwAccount = accountMap.get("KRW");
        double totalCash = Double.parseDouble(krwAccount.getBalance()) + askAmount;
        krwAccount.setBalance(ApplicationUtil.toNumberString(totalCash));
        coinAccount.setBalance("0");
        coinAccount.setAvgBuyPrice(null);

        backtestRow.setBuyTotalAmount(BacktestHelper.getBuyTotalAmount(accountMap));
        backtestRow.setTradeEvent(TradeType.SELL);
        backtestRow.setAskPrice(tradePrice);
        backtestRow.setCash(krwAccount.getBalanceValue());
        backtestRow.setAskReason(reason);
        backtestRow.setMaShort(currentPrice.getMaShort());
        backtestRow.setMaLong(currentPrice.getMaLong());
        backtestRow.setHighYield(highYield);
        backtestRow.setLowYield(lowYield);

        tradeHistory.add(backtestRow);
    }

    @Override
    public void highYield(String market, double highYield) {
        this.highYield = highYield;
    }

    @Override
    public void lowYield(String market, double lowYield) {
        this.lowYield = lowYield;
    }
}
