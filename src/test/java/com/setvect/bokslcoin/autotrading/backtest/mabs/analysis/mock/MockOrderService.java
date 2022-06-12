package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.mock;

import com.setvect.bokslcoin.autotrading.AccessTokenMaker;
import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import com.setvect.bokslcoin.autotrading.exchange.OrderService;
import com.setvect.bokslcoin.autotrading.model.OrderChance;
import com.setvect.bokslcoin.autotrading.model.OrderHistory;
import com.setvect.bokslcoin.autotrading.model.OrderResult;

import java.util.Collections;
import java.util.List;

public class MockOrderService extends OrderService {
    public MockOrderService(AccessTokenMaker accessInfo, ConnectionInfo connectionInfo) {
        super(accessInfo, connectionInfo);
    }

    @Override
    public OrderChance getChange(String market) {
        return null;
    }

    @Override
    public List<OrderHistory> getHistory(int page, int limit) {
        // 백테스트에서는 의미 없는 기능, 빈 리스트 넒기
        return Collections.emptyList();
    }

    @Override
    public OrderHistory cancelOrder(String uuid) {
        return null;
    }

    @Override
    public OrderResult callOrderBid(String market, String volume, String price) {
        return null;
    }

    @Override
    public OrderResult callOrderAsk(String market, String volume, String price) {
        return null;
    }

    @Override
    public OrderResult callOrderBidByMarket(String market, String price) {
        return null;
    }

    @Override
    public OrderResult callOrderAskByMarket(String market, String volume) {
        return null;
    }

}
