package com.setvect.bokslcoin.autotrading.algorithm;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TradePeriod {
    P_60(55, 4, 1),
    P_240(230, 9, 1),
    P_1440(1410, 29, 1);
    /**
     * 매수 기간(분)
     */
    int bidMinute;
    /**
     * 매수, 매도 사이에 매매가 일어나지 않는 시간(분)
     */
    int intermissionMinute;
    /**
     * 매도 기간(분)
     */
    int askMinute;

    public int getTotal() {
        return bidMinute + intermissionMinute + askMinute;
    }
}
