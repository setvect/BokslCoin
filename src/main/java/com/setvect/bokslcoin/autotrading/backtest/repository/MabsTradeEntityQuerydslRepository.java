package com.setvect.bokslcoin.autotrading.backtest.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.QMabsTradeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// TODO MabsConditionEntityRepository 통합
@Repository
@RequiredArgsConstructor
public class MabsTradeEntityQuerydslRepository {
    private final JPAQueryFactory queryFactory;

    @Transactional
    public long deleteByConditionId(List<Integer> conditionId) {
        return queryFactory.delete(QMabsTradeEntity.mabsTradeEntity)
                .where(QMabsTradeEntity.mabsTradeEntity.conditionEntity.conditionSeq.in(conditionId))
                .execute();
    }
}