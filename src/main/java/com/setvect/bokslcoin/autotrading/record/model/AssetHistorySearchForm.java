package com.setvect.bokslcoin.autotrading.record.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AssetHistorySearchForm {
    /**
     * 자산 종류
     * KRW, BTC, ETH, ...
     */
    private String currency;

    /**
     * 시작 날짜
     */
    private LocalDateTime from;

    /**
     * 종료 날짜
     */
    private LocalDateTime to;

}
