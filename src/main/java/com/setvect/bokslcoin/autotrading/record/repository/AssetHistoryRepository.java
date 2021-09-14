package com.setvect.bokslcoin.autotrading.record.repository;

import com.setvect.bokslcoin.autotrading.record.entity.AssetHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetHistoryRepository extends JpaRepository<AssetHistoryEntity, Integer> {

}
