package com.setvect.bokslcoin.autotrading.backtest.mabs.multi;

import com.setvect.bokslcoin.autotrading.backtest.entity.CandleEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepository;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @deprecated "com.setvect.bokslcoin.autotrading.backtest.mabs.analysis" 패키지 사용
 */
@Deprecated
@Slf4j
public class CandleDataProvider {
    private final CandleRepository candleRepository;
    /**
     * UTC 기준
     */
    private LocalDateTime currentDateTime;

    private CandleMinute currentCandle;


    private Map<CacheKey, List<Candle>> cachePeriod = new HashMap();

    public CandleDataProvider(CandleRepository candleRepository) {
        this.candleRepository = candleRepository;
    }

    /**
     * @param current UTC 기준
     */
    public void setCurrentTime(LocalDateTime current) {
        this.currentDateTime = current;
    }

    public CandleMinute getCurrentCandle(String market) {
        List<CandleEntity> minuteOfDay = candleRepository.findMarketPrice(market, PeriodType.PERIOD_1, currentDateTime, currentDateTime);
        if (minuteOfDay.size() == 0) {
            currentCandle = null;
        } else {
            currentCandle = ApplicationUtil.getMapper().map(minuteOfDay.get(0), CandleMinute.class);
        }
        return currentCandle;
    }

    public List<CandleDay> beforeDayCandle(String market, int count) {
        List<Candle> r = beforeData(market, PeriodType.PERIOD_1440, count);
        List<CandleDay> result = r.stream().map(p -> ApplicationUtil.getMapper().map(p, CandleDay.class)).collect(Collectors.toList());
        return result;
    }

    public List<CandleMinute> beforeMinute(String market, PeriodType periodType, Integer count) {
        List<Candle> r = beforeData(market, periodType, count);
        List<CandleMinute> result = r.stream().map(p -> ApplicationUtil.getMapper().map(p, CandleMinute.class)).collect(Collectors.toList());
        return result;
    }

    public List<Candle> beforeData(String market, PeriodType periodType, Integer count) {
        if (cachePeriod.size() > 1000) {
            log.info("cache clear");
            cachePeriod.clear();
        }
        LocalDateTime base = null;
        if (periodType == PeriodType.PERIOD_1440) {
            base = currentDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
        } else if (periodType == PeriodType.PERIOD_240) {
            int hour = (currentDateTime.getHour() / 4) * 4;
            base = currentDateTime.withHour(hour).withMinute(0).withSecond(0).withNano(0);
        } else if (periodType == PeriodType.PERIOD_60) {
            base = currentDateTime.withMinute(0).withSecond(0).withNano(0);
        } else if (periodType == PeriodType.PERIOD_30) {
            int minute = (currentDateTime.getMinute() / 30) * 30;
            base = currentDateTime.withMinute(minute).withSecond(0).withNano(0);
        } else if (periodType == PeriodType.PERIOD_15) {
            int minute = (currentDateTime.getMinute() / 15) * 15;
            base = currentDateTime.withMinute(minute).withSecond(0).withNano(0);
        }
        CacheKey key = CacheKey.builder().market(market).count(count).base(base).period(periodType).build();
        List<Candle> periodData = cachePeriod.get(key);
        if (periodData == null) {
            List<CandleEntity> candleList = candleRepository.findMarketPricePeriod(market, periodType, base, PageRequest.of(0, count - 1));
            periodData = candleList.stream().map(v -> ApplicationUtil.getMapper().map(v, CandleDay.class)).collect(Collectors.toList());
            periodData.add(0, ApplicationUtil.getMapper().map(currentCandle, CandleDay.class));
            cachePeriod.put(key, periodData);
        }
        periodData.remove(0);
        periodData.add(0, ApplicationUtil.getMapper().map(currentCandle, CandleDay.class));
        return periodData;
    }

    @Builder
    @EqualsAndHashCode
    static class CacheKey {
        private final String market;
        private final int count;
        private final LocalDateTime base;
        private final PeriodType period;
    }


}
