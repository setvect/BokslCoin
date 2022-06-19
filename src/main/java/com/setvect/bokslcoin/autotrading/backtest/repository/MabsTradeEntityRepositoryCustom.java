package com.setvect.bokslcoin.autotrading.backtest.repository;

import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsTradeEntity;

import java.util.List;

public interface MabsTradeEntityRepositoryCustom {

    /**
     * @param mabsConditionSeq �м� ���� �Ϸù�ȣ
     * @return �ŷ� ������ ��¥�� ��������
     */
    List<MabsTradeEntity> findByCondition(int mabsConditionSeq);

    long deleteTradeByConditionId(List<Integer> conditionId);
}
