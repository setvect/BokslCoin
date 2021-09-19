package com.setvect.bokslcoin.autotrading.record.repository;

import com.setvect.bokslcoin.autotrading.record.model.AssetHistoryDto;
import com.setvect.bokslcoin.autotrading.record.model.AssetHistorySearchForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssetHistoryRepositoryCustom {
    Page<AssetHistoryDto> pageAssetHistory(AssetHistorySearchForm searchForm, Pageable pageable);
}
