package com.setvect.bokslcoin.autotrading.backtest.repository;

import com.setvect.bokslcoin.autotrading.backtest.entity.neovbs.NeoVbsTradeEntity;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NeoVbsTradeEntityRepositoryCustom {
    List<NeoVbsTradeEntity> findByCondition(@Param("vbsConditionSeq") int vbsConditionSeq);


    long deleteTradeByConditionId(List<Integer> conditionId);
}
