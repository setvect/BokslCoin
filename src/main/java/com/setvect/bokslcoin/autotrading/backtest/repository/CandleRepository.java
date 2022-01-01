package com.setvect.bokslcoin.autotrading.backtest.repository;

import com.setvect.bokslcoin.autotrading.backtest.entity.CandleEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import org.springframework.data.domain.Pageable;
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
     * @param periodType 주기
     * @param from       시작 날짜 - UTC
     * @param end        종료 날짜 - UTC
     * @return 시세
     */
    @Query("select c from WA_CANDLE c " +
            " where c.market = :market and c.periodType = :period and c.candleDateTimeUtc between :from and :end order by c.candleDateTimeUtc")
    List<CandleEntity> findMarketPrice(@Param("market") String market, @Param("period") PeriodType periodType,
                                       @Param("from") LocalDateTime from, @Param("end") LocalDateTime end);

    /**
     * 기준을 보다 작은 시세 정보
     *
     * @param market     코인
     * @param periodType 주기
     * @param base       시작 날짜 - 해당 날짜를 포함하지 않음 - UTC
     * @param pageable   가져올 갯수
     * @return 시세
     */
    @Query("select c from WA_CANDLE c " +
            " where c.market = :market and c.periodType = :period and c.candleDateTimeUtc < :base order by c.candleDateTimeUtc desc")
    List<CandleEntity> findMarketPricePeriod(@Param("market") String market, @Param("period") PeriodType periodType,
                                             @Param("base") LocalDateTime base, Pageable pageable);
}
