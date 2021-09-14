package com.setvect.bokslcoin.autotrading.record.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TradeSearchForm {
    /**
     * 코인 이름TradeSearchForm
     * KRW-BTC, KRW-ETH, ...
     */
    private String market;

    /**
     * 시작 날짜
     */
    private LocalDateTime from;

    /**
     * 종료 날짜
     */
    private LocalDateTime to;

}
