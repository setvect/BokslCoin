package com.setvect.bokslcoin.autotrading.record.repository;

import com.setvect.bokslcoin.autotrading.record.entity.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends JpaRepository<TradeEntity, Integer>, TradeRepositoryCustom {

}
