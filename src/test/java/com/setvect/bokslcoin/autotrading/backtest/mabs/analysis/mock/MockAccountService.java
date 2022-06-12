package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.mock;

import com.setvect.bokslcoin.autotrading.exchange.AccountService;

public class MockAccountService extends AccountService {
    public MockAccountService() {
        super(new MockAccessTokenMaker(), new MockConnectionInfo());
    }
}
