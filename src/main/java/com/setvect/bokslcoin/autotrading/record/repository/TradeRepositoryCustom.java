package com.setvect.bokslcoin.autotrading.record.repository;

import com.setvect.bokslcoin.autotrading.record.model.TradeDto;
import com.setvect.bokslcoin.autotrading.record.model.TradeSearchForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TradeRepositoryCustom {
    Page<TradeDto> pageArticle(TradeSearchForm searchForm, Pageable pageable);
}
