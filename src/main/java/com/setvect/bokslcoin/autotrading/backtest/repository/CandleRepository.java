package com.setvect.bokslcoin.autotrading.backtest.repository;

import com.setvect.bokslcoin.autotrading.backtest.entity.CandleEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CandleRepository extends JpaRepository<CandleEntity, Integer> {

    /**
     * @param market     코인
     * @param from       UTC 기준
     * @param end        UTC 기준
     * @param periodType 주기
     */
    @Query("select c from CANDLE c " +
            " where c.market = :market and c.periodType = :period and c.candleDateTimeUtc between :from and :end")
    List<CandleEntity> findMarketPrice(@Param("market") String market, @Param("period") PeriodType periodType,
                                       @Param("from") LocalDateTime from, @Param("end") LocalDateTime end);
}
