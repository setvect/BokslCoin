package com.setvect.bokslcoin.autotrading.backtest.repository;

import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsTradeEntity;

import java.util.List;

public interface MabsTradeEntityRepositoryCustom {

    /**
     * @param mabsConditionSeq 분석 조건 일련번호
     * @return 거래 내역을 날짜순 오름차순
     */
    List<MabsTradeEntity> findByCondition(int mabsConditionSeq);

    long deleteTradeByConditionId(List<Integer> conditionId);
}
