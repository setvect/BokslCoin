package com.setvect.bokslcoin.autotrading.backtest.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.setvect.bokslcoin.autotrading.backtest.entity.neovbs.NeoVbsTradeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.setvect.bokslcoin.autotrading.backtest.entity.neovbs.QNeoVbsTradeEntity.neoVbsTradeEntity;

@Repository
@RequiredArgsConstructor
public class NeoVbsTradeEntityRepositoryImpl implements NeoVbsTradeEntityRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    /**
     * @param neoVbsConditionSeq �м� ���� �Ϸù�ȣ
     * @return �ŷ� ������ ��¥�� ��������
     */
    public List<NeoVbsTradeEntity> findByCondition(@Param("neoVbsConditionSeq") int neoVbsConditionSeq) {
        return queryFactory
                .select(neoVbsTradeEntity)
                .where(neoVbsTradeEntity.vbsConditionEntity.vbsConditionSeq.eq(neoVbsConditionSeq))
                .orderBy(neoVbsTradeEntity.tradeTimeKst.asc())
                .fetch();
    }

    @Override
    @Transactional
    public long deleteTradeByConditionId(List<Integer> conditionId) {
        return queryFactory.delete(neoVbsTradeEntity)
                .where(neoVbsTradeEntity.vbsConditionEntity.vbsConditionSeq.in(conditionId))
                .execute();
    }

}
