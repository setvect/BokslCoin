package com.setvect.bokslcoin.autotrading.model;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class CandleMinute extends Candle {
    /**
     * 분 단위(유닛)
     */
    private Integer unit;
}
