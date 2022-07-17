package com.setvect.bokslcoin.autotrading.backtest.neovbs.mock;

import com.setvect.bokslcoin.autotrading.algorithm.AskReason;
import com.setvect.bokslcoin.autotrading.algorithm.BasicTradeEvent;
import com.setvect.bokslcoin.autotrading.backtest.common.BacktestHelper;
import com.setvect.bokslcoin.autotrading.backtest.neovbs.model.NeoVbsMultiBacktestRow;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;


public class MockNeoVbsTradeEvent extends BasicTradeEvent {

    @Setter
    private Map<String, Account> accountMap;
    @Setter
    private List<NeoVbsMultiBacktestRow> tradeHistory;
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
    private Candle candle;

    private double targetPrice;

    public MockNeoVbsTradeEvent(SlackMessageService slackMessageService) {
        super(slackMessageService);
    }

    @Override
    public void check(Candle candle) {
        this.candle = candle;
    }

    @Override
    public void setTargetPrice(String market, double targetPrice) {
        this.targetPrice = targetPrice;
    }

    @Override
    public void bid(String market, double tradePrice, double bidPrice) {
        NeoVbsMultiBacktestRow backtestRow = new NeoVbsMultiBacktestRow(BacktestHelper.depthCopy(candle));

        Account coinAccount = accountMap.get(market);
        coinAccount.setAvgBuyPrice(ApplicationUtil.toNumberString(tradePrice));

        Account krwAccount = accountMap.get("KRW");
        double cash = Double.parseDouble(krwAccount.getBalance()) - bidPrice;
        krwAccount.setBalance(ApplicationUtil.toNumberString(cash));

        String balance = ApplicationUtil.toNumberString(bidPrice / tradePrice);
        coinAccount.setBalance(balance);

        backtestRow.setTargetPrice(targetPrice);
        backtestRow.setTradeEvent(TradeType.BUY);
        backtestRow.setBidPrice(tradePrice);
        backtestRow.setBuyAmount(bidPrice);
        backtestRow.setBuyTotalAmount(BacktestHelper.getBuyTotalAmount(accountMap));
        backtestRow.setCash(cash);

        tradeHistory.add(backtestRow);
    }

    @Override
    public void ask(String market, double balance, double tradePrice, AskReason reason) {
        NeoVbsMultiBacktestRow backtestRow = new NeoVbsMultiBacktestRow(BacktestHelper.depthCopy(candle));

        Account coinAccount = accountMap.get(market);
        backtestRow.setBidPrice(coinAccount.getAvgBuyPriceValue());
        backtestRow.setBuyAmount(coinAccount.getInvestCash());

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