package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.mock;

import com.setvect.bokslcoin.autotrading.AccessTokenMaker;
import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import com.setvect.bokslcoin.autotrading.exchange.OrderService;

public class MockOrderService extends OrderService {
    public MockOrderService(AccessTokenMaker accessInfo, ConnectionInfo connectionInfo) {
        super(accessInfo, connectionInfo);
    }
}
