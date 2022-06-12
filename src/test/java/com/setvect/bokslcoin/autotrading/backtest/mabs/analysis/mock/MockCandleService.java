package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.mock;

import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.CandleDataProvider;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockCandleService extends CandleService {
    private final static Map<Integer, PeriodType> PERIOD_TYPE = new HashMap<>();

    static {
        PERIOD_TYPE.put(15, PeriodType.PERIOD_15);
        PERIOD_TYPE.put(30, PeriodType.PERIOD_30);
        PERIOD_TYPE.put(60, PeriodType.PERIOD_60);
        PERIOD_TYPE.put(240, PeriodType.PERIOD_240);
    }

    @Setter
    private CandleDataProvider candleDataProvider;

    public MockCandleService(ConnectionInfo connectionInfo) {
        super(connectionInfo);
    }

    @Override
    public CandleMinute getMinute(int unit, String market) {
        return candleDataProvider.getCurrentCandle(market);
    }

    @Override
    public List<CandleMinute> getMinute(int unit, String market, int count) {
        return candleDataProvider.beforeMinute(market, PERIOD_TYPE.get(unit), count);
    }

    @Override
    public CandleDay getDay(String market) {
        List<CandleDay> candleDays = candleDataProvider.beforeDayCandle(market, 2);
        return candleDays.get(0);
    }

    @Override
    public List<CandleDay> getDay(String market, int count) {
        return candleDataProvider.beforeDayCandle(market, count);
    }

}
