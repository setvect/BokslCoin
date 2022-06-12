package com.setvect.bokslcoin.autotrading.backtest.repository;

import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsTradeEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.neovbs.NeoVbsTradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NeoVbsTradeEntityRepository extends JpaRepository<NeoVbsTradeEntity, Integer> {
    /**
     * @param vbsConditionSeq 분석 조건 일련번호
     * @return 거래 내역을 날짜순 오름차순
     */
    @Query("select x from XD_VBS_TRADE x where x.vbsConditionEntity.vbsConditionSeq = :vbsConditionSeq order by x.tradeTimeKst")
    List<MabsTradeEntity> findByCondition(@Param("vbsConditionSeq") int vbsConditionSeq);

}