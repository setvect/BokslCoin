package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.mock;

import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.common.TradeCommonService;
import com.setvect.bokslcoin.autotrading.algorithm.mabs.MabsMultiProperties;
import com.setvect.bokslcoin.autotrading.algorithm.mabs.MabsMultiService;
import lombok.Getter;

public class MockMabsMultiService extends MabsMultiService {
    @Getter
    private final MabsMultiProperties properties;

    public MockMabsMultiService(TradeCommonService tradeCommonService, TradeEvent tradeEvent, MabsMultiProperties properties) {
        super(tradeCommonService, tradeEvent, properties);
        this.properties = properties;
    }

}
