package com.setvect.bokslcoin.autotrading.record.repository;

import com.setvect.bokslcoin.autotrading.record.model.AssetHistoryDto;
import com.setvect.bokslcoin.autotrading.record.model.AssetHistorySearchForm;
import com.setvect.bokslcoin.autotrading.record.model.AssetPeriodHistoryDto;
import com.setvect.bokslcoin.autotrading.record.model.AssetPeriodHistorySearchForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssetHistoryRepositoryCustom {
    Page<AssetHistoryDto> pageAssetHistory(AssetHistorySearchForm searchForm, Pageable pageable);

    Page<AssetPeriodHistoryDto> pageAssetPeriodHistory(AssetPeriodHistorySearchForm searchForm, Pageable pageable);
}
