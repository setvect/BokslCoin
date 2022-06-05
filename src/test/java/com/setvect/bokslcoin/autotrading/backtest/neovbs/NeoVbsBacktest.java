package com.setvect.bokslcoin.autotrading.backtest.neovbs;

import com.setvect.bokslcoin.autotrading.algorithm.mabs.MabsMultiService;
import com.setvect.bokslcoin.autotrading.algorithm.neovbs.NeoVbsMultiService;
import com.setvect.bokslcoin.autotrading.algorithm.websocket.TradeResult;
import com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.CandleDataProvider;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepository;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class NeoVbsBacktest {
    @InjectMocks
    private NeoVbsMultiService neoVbsMultiService;

    @Autowired
    private CandleRepository candleRepository;

    @Test
    public void 백테스트() {
        DateRange range = new DateRange("2022-01-01T00:00:00", "2022-02-01T00:00:00");
        CandleDataProvider candleDataProvider = new CandleDataProvider(candleRepository);
        LocalDateTime current = range.getFrom();
        LocalDateTime to = range.getTo();

        while (current.isBefore(to) || current.equals(to)) {
            current = current.plusMinutes(1);
            candleDataProvider.setCurrentTime(current);
            CandleMinute candle = candleDataProvider.getCurrentCandle("KRW-BTC");

            TradeResult tradeResult = TradeResult.builder()
                    .type("trade")
                    .code(candle.getMarket())
                    .tradePrice(candle.getTradePrice())
                    .tradeDate(candle.getCandleDateTimeUtc().toLocalDate())
                    .tradeTime(candle.getCandleDateTimeUtc().toLocalTime())
                    // 백테스트에서는 의미없는값
                    .timestamp(0L)
                    .prevClosingPrice(0)
                    .tradeVolume(0)
                    .build();

            neoVbsMultiService.tradeEvent(tradeResult);

        }
    }
}
