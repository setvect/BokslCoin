package com.setvect.bokslcoin.autotrading.backtest.repository;

import com.setvect.bokslcoin.autotrading.backtest.entity.neovbs.NeoVbsTradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NeoVbsTradeEntityRepository extends JpaRepository<NeoVbsTradeEntity, Integer>, NeoVbsTradeEntityRepositoryCustom {

}