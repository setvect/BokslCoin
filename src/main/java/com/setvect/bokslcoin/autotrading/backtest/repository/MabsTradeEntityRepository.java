package com.setvect.bokslcoin.autotrading.backtest.repository;

import com.setvect.bokslcoin.autotrading.backtest.entity.MabsTradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MabsTradeEntityRepository extends JpaRepository<MabsTradeEntity, Integer> {
}