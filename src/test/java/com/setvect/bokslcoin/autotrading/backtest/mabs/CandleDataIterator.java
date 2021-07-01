package com.setvect.bokslcoin.autotrading.backtest.mabs;

import com.setvect.bokslcoin.autotrading.backtest.entity.CandleEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepository;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class CandleDataIterator implements Iterator<CandleMinute> {

    private final BaseCondition condition;
    private final CandleRepository candleRepository;
    private LocalDateTime nextDate;
    private Iterator<CandleEntity> currentCandleIterator;

    public CandleDataIterator(BaseCondition condition, CandleRepository candleRepository) {
        this.condition = condition;
        nextDate = condition.getRange().getFrom();
        this.candleRepository = candleRepository;
        currentCandleIterator = Collections.emptyIterator();
    }

    @Override
    public boolean hasNext() {
        if (!condition.getRange().isBetween(nextDate)) {
            return false;
        }
        if (!currentCandleIterator.hasNext()) {
            List<CandleEntity> minuteOfDay = candleRepository.findMarketPrice(condition.getMarket(), PeriodType.PERIOD_1, nextDate, nextDate.plusDays(1).minusSeconds(1));
            currentCandleIterator = minuteOfDay.listIterator();
            nextDate = nextDate.plusDays(1);
        }
        return currentCandleIterator.hasNext();
    }

    @Override
    public CandleMinute next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        CandleEntity current = currentCandleIterator.next();
        return ApplicationUtil.getMapper().map(current, CandleMinute.class);
    }
}
