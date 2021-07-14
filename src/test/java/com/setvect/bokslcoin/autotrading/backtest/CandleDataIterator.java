package com.setvect.bokslcoin.autotrading.backtest;

import com.setvect.bokslcoin.autotrading.backtest.entity.CandleEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepository;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.LapTimeChecker;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
public class CandleDataIterator implements Iterator<CandleMinute> {

    private final CandleRepository candleRepository;
    private LocalDateTime nextDate;
    private Iterator<CandleEntity> currentCandleIterator;
    private CandleMinute current;
    private LapTimeChecker ck = new LapTimeChecker("backtest");
    private Map<CacheKey, List<Candle>> cachePeriod = new HashMap();
    private DateRange range;
    private String market;

    public CandleDataIterator(String market, DateRange range, CandleRepository candleRepository) {
        this.market = market;
        this.range = range;
        nextDate = range.getFrom();
        this.candleRepository = candleRepository;
        currentCandleIterator = Collections.emptyIterator();
    }

    @Override
    public boolean hasNext() {
        if (current != null && !range.isBetween(current.getCandleDateTimeUtc())) {
            return false;
        }
        // 한달씩 로딩
        if (!currentCandleIterator.hasNext()) {
            log.info(String.format("load Year: %d, Month: %d", nextDate.getYear(), nextDate.getMonthValue()));
            LocalDateTime end = nextDate.plusMonths(1).minusSeconds(1);
            List<CandleEntity> minuteOfDay = candleRepository.findMarketPrice(market, PeriodType.PERIOD_1, nextDate, end);
            currentCandleIterator = minuteOfDay.listIterator();
            nextDate = nextDate.plusMonths(1);
            cachePeriod.clear();
        }
        return currentCandleIterator.hasNext();
    }

    @Override
    public CandleMinute next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        CandleEntity next = currentCandleIterator.next();
        current = ApplicationUtil.getMapper().map(next, CandleMinute.class);
        return current;
    }

    public CandleMinute getCurrentCandle() {
        return current;
    }


    public List<CandleDay> beforeDayCandle(int count) {
        List<Candle> r = beforeData(PeriodType.PERIOD_1440, count);
        List<CandleDay> result = r.stream().map(p -> ApplicationUtil.getMapper().map(p, CandleDay.class)).collect(Collectors.toList());
        return result;
    }

    public List<CandleMinute> beforeMinute(PeriodType periodType, Integer count) {
        List<Candle> r = beforeData(periodType, count);
        List<CandleMinute> result = r.stream().map(p -> ApplicationUtil.getMapper().map(p, CandleMinute.class)).collect(Collectors.toList());
        return result;
    }

    public List<Candle> beforeData(PeriodType periodType, Integer count) {
        LocalDateTime base = null;
        if (periodType == PeriodType.PERIOD_1440) {
            base = current.getCandleDateTimeUtc().withHour(0).withMinute(0).withSecond(0).withNano(0);
        } else if (periodType == PeriodType.PERIOD_240) {
            int hour = (current.getCandleDateTimeUtc().getHour() / 4) * 4;
            base = current.getCandleDateTimeUtc().withHour(hour).withMinute(0).withSecond(0).withNano(0);
        } else if (periodType == PeriodType.PERIOD_60) {
            base = current.getCandleDateTimeUtc().withMinute(0).withSecond(0).withNano(0);
        }
        CacheKey key = CacheKey.builder().market(current.getMarket()).count(count).base(base).period(periodType).build();
        List<Candle> periodData = cachePeriod.get(key);
        if (periodData == null) {
            List<CandleEntity> candleList = candleRepository.findMarketPricePeriod(current.getMarket(), periodType, base, PageRequest.of(0, count - 1));
            periodData = candleList.stream().map(v -> ApplicationUtil.getMapper().map(v, CandleDay.class)).collect(Collectors.toList());
            periodData.add(0, ApplicationUtil.getMapper().map(current, CandleDay.class));
            cachePeriod.put(key, periodData);
        }
        periodData.remove(0);
        periodData.add(0, ApplicationUtil.getMapper().map(current, CandleDay.class));
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
