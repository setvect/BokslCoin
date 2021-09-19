package com.setvect.bokslcoin.autotrading.record.controller;

import com.setvect.bokslcoin.autotrading.record.model.AssetHistoryDto;
import com.setvect.bokslcoin.autotrading.record.model.AssetHistorySearchForm;
import com.setvect.bokslcoin.autotrading.record.model.CommonResponse;
import com.setvect.bokslcoin.autotrading.record.repository.AssetHistoryRepository;
import com.setvect.bokslcoin.autotrading.record.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 거래내역
 */
@RestController
@RequiredArgsConstructor
public class TradeHistoryController {
    private final AssetHistoryRepository assetHistoryRepository;
    private final TradeRepository tradeRepository;

    @GetMapping("/trade/page")
    public CommonResponse<Page<AssetHistoryDto>> pageAssetHistory(AssetHistorySearchForm searchForm, Pageable pageable){
        Page<AssetHistoryDto> result = assetHistoryRepository.pageAssetHistory(searchForm, pageable);
        return new CommonResponse<>(result);
    }
}
