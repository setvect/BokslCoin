package com.setvect.bokslcoin.autotrading.backtest.neovbs.mock;

import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.common.TradeCommonService;
import com.setvect.bokslcoin.autotrading.algorithm.neovbs.NeoVbsMultiProperties;
import com.setvect.bokslcoin.autotrading.algorithm.neovbs.NeoVbsMultiService;
import lombok.Getter;

public class MockNeoVbsMultiService extends NeoVbsMultiService {
    @Getter
    private final NeoVbsMultiProperties properties;

    public MockNeoVbsMultiService(TradeCommonService tradeCommonService, TradeEvent tradeEvent, NeoVbsMultiProperties properties) {
        super(tradeCommonService, tradeEvent, properties);
        this.properties = properties;
    }

}
