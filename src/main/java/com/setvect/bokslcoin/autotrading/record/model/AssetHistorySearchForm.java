package com.setvect.bokslcoin.autotrading.record.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetHistorySearchForm extends RangeForm {
    /**
     * 자산 종류
     * KRW, BTC, ETH, ...
     */
    private String currency;


}
