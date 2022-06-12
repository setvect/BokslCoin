package com.setvect.bokslcoin.autotrading.backtest.repository;

import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsTradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MabsTradeEntityRepository extends JpaRepository<MabsTradeEntity, Integer> {
    /**
     * @param mabsConditionSeq 분석 조건 일련번호
     * @return 거래 내역을 날짜순 오름차순
     */
    @Query("select x from XB_MABS_TRADE x where x.conditionEntity.conditionSeq = :mabsConditionSeq order by x.tradeTimeKst")
    List<MabsTradeEntity> findByCondition(@Param("mabsConditionSeq") int mabsConditionSeq);

}