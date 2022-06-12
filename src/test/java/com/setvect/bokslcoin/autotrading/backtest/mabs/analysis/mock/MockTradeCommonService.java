package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.mock;

import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.common.TradeCommonService;
import com.setvect.bokslcoin.autotrading.exchange.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.OrderService;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import com.setvect.bokslcoin.autotrading.record.repository.AssetHistoryRepository;
import com.setvect.bokslcoin.autotrading.record.repository.TradeRepository;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;

public class MockTradeCommonService extends TradeCommonService {
    public MockTradeCommonService(TradeEvent tradeEvent,
                                  AccountService accountService,
                                  OrderService orderService,
                                  CandleService candleService,
                                  TradeRepository tradeRepository,
                                  SlackMessageService slackMessageService,
                                  AssetHistoryRepository assetHistoryRepository) {
        super(tradeEvent, accountService, orderService, candleService, tradeRepository, slackMessageService, assetHistoryRepository);
    }
}
