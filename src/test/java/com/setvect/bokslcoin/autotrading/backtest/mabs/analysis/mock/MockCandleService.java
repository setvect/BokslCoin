package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.mock;

import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;

public class MockCandleService extends CandleService {
    public MockCandleService(ConnectionInfo connectionInfo) {
        super(connectionInfo);
    }
}
