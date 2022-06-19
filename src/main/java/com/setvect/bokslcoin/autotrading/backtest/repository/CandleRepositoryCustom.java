package com.setvect.bokslcoin.autotrading.backtest.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.setvect.bokslcoin.autotrading.backtest.entity.CandleEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.setvect.bokslcoin.autotrading.backtest.entity.QCandleEntity.candleEntity;

@Repository
@RequiredArgsConstructor
public class CandleRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    /**
     * @param market     코인
     * @param periodType 주기
     * @param from       시작 날짜 - UTC
     * @param end        종료 날짜 - UTC
     * @return 시세(날짜 기준 오름 차순)
     */
    public List<CandleEntity> findMarketPrice(String market, PeriodType periodType, LocalDateTime from, LocalDateTime end) {
        return queryFactory.select(candleEntity)
                .from(candleEntity)
                .where(
                        candleEntity.market.eq(market)
                                .and(candleEntity.periodType.eq(periodType))
                                .and(candleEntity.candleDateTimeUtc.between(from, end))
                )
                .orderBy(candleEntity.candleDateTimeUtc.asc())
                .fetch();
    }

    /**
     * 기준을 날짜 보다 이전 시세 정보
     *
     * @param market     코인
     * @param periodType 주기
     * @param base       시작 날짜 - 해당 날짜를 포함하지 않음 - UTC
     * @param pageable   가져올 갯수
     * @return 시세(날짜 기준 내림 차순)
     */
    public List<CandleEntity> findMarketPricePeriodBefore(String market, PeriodType periodType, LocalDateTime base, Pageable pageable) {
        return queryFactory.select(candleEntity)
                .from(candleEntity)
                .where(
                        candleEntity.market.eq(market)
                                .and(candleEntity.periodType.eq(periodType))
                                .and(candleEntity.candleDateTimeUtc.lt(base))
                )
                .orderBy(candleEntity.candleDateTimeUtc.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    /**
     * 기준을 날짜 보다 이후 시세 정보
     *
     * @param market     코인
     * @param periodType 주기
     * @param base       시작 날짜 - 해당 날짜를 포함하지 않음 - UTC
     * @param pageable   가져올 갯수
     * @return 시세(날짜 기준 오름 차순)
     */
    public List<CandleEntity> findMarketPricePeriodAfter(String market, PeriodType periodType, LocalDateTime base, Pageable pageable) {
        return queryFactory.select(candleEntity)
                .from(candleEntity)
                .where(
                        candleEntity.market.eq(market)
                                .and(candleEntity.periodType.eq(periodType))
                                .and(candleEntity.candleDateTimeUtc.gt(base))
                )
                .orderBy(candleEntity.candleDateTimeUtc.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}
