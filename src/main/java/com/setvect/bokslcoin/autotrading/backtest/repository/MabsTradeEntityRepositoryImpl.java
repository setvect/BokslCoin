package com.setvect.bokslcoin.autotrading.backtest.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsTradeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.setvect.bokslcoin.autotrading.backtest.entity.mabs.QMabsTradeEntity.mabsTradeEntity;

@Repository
@RequiredArgsConstructor
public class MabsTradeEntityRepositoryImpl implements MabsTradeEntityRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    /**
     * @param mabsConditionSeq 분석 조건 일련번호
     * @return 거래 내역을 날짜순 오름차순
     */
    public List<MabsTradeEntity> findByCondition(@Param("mabsConditionSeq") int mabsConditionSeq) {
        return queryFactory
                .select(mabsTradeEntity)
                .from(mabsTradeEntity)
                .where(mabsTradeEntity.conditionEntity.conditionSeq.eq(mabsConditionSeq))
                .orderBy(mabsTradeEntity.tradeTimeKst.asc())
                .fetch();
    }

    @Override
    @Transactional
    public long deleteTradeByConditionId(List<Integer> conditionId) {
        return queryFactory.delete(mabsTradeEntity)
                .where(mabsTradeEntity.conditionEntity.conditionSeq.in(conditionId))
                .execute();
    }
}