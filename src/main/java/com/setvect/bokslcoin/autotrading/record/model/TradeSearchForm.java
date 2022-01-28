package com.setvect.bokslcoin.autotrading.record.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TradeSearchForm extends RangeForm {
    /**
     * 코인 이름
     * KRW-BTC, KRW-ETH, ...
     */
    private String market;


}
