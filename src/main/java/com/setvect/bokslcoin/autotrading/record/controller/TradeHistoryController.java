package com.setvect.bokslcoin.autotrading.record.controller;

import com.setvect.bokslcoin.autotrading.record.model.AssetHistoryDto;
import com.setvect.bokslcoin.autotrading.record.model.AssetHistorySearchForm;
import com.setvect.bokslcoin.autotrading.record.model.AssetPeriodHistoryDto;
import com.setvect.bokslcoin.autotrading.record.model.AssetPeriodHistorySearchForm;
import com.setvect.bokslcoin.autotrading.record.model.CommonResponse;
import com.setvect.bokslcoin.autotrading.record.model.TradeDto;
import com.setvect.bokslcoin.autotrading.record.model.TradeSearchForm;
import com.setvect.bokslcoin.autotrading.record.repository.AssetHistoryRepository;
import com.setvect.bokslcoin.autotrading.record.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 거래내역, 자산 내역
 */
@RestController
@RequiredArgsConstructor
public class TradeHistoryController {
    private final AssetHistoryRepository assetHistoryRepository;
    private final TradeRepository tradeRepository;

    /**
     * @param searchForm 검색조건
     * @param pageable   페이징
     * @return 종목별 자산 내역
     */
    @GetMapping("/assetHistory/page")
    public CommonResponse<Page<AssetHistoryDto>> pageAssetHistory(AssetHistorySearchForm searchForm, Pageable pageable) {
        Page<AssetHistoryDto> result = assetHistoryRepository.pageAssetHistory(searchForm, pageable);
        return new CommonResponse<>(result);
    }

    /**
     * @param searchForm 검색조건
     * @param pageable   페이징
     * @return 거래주기별 자산 합산 내역
     */
    @GetMapping("/assetPeriodHistory/page")
    public CommonResponse<Page<AssetPeriodHistoryDto>> pageAssetHistory(AssetPeriodHistorySearchForm searchForm, Pageable pageable) {
        Page<AssetPeriodHistoryDto> result = assetHistoryRepository.pageAssetPeriodHistory(searchForm, pageable);
        return new CommonResponse<>(result);
    }

    /**
     * @param searchForm 검색 조건
     * @param pageable   페이징
     * @return 거래내역
     */
    @GetMapping("/trade/page")
    public CommonResponse<Page<TradeDto>> pageTradeList(TradeSearchForm searchForm, Pageable pageable) {
        Page<TradeDto> result = tradeRepository.pageTrade(searchForm, pageable);
        return new CommonResponse<>(result);
    }
}
