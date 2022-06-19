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
     * @param market     ����
     * @param periodType �ֱ�
     * @param from       ���� ��¥ - UTC
     * @param end        ���� ��¥ - UTC
     * @return �ü�(��¥ ���� ���� ����)
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
     * ������ ��¥ ���� ���� �ü� ����
     *
     * @param market     ����
     * @param periodType �ֱ�
     * @param base       ���� ��¥ - �ش� ��¥�� �������� ���� - UTC
     * @param pageable   ������ ����
     * @return �ü�(��¥ ���� ���� ����)
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
     * ������ ��¥ ���� ���� �ü� ����
     *
     * @param market     ����
     * @param periodType �ֱ�
     * @param base       ���� ��¥ - �ش� ��¥�� �������� ���� - UTC
     * @param pageable   ������ ����
     * @return �ü�(��¥ ���� ���� ����)
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
