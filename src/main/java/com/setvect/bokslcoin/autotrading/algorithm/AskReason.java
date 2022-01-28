package com.setvect.bokslcoin.autotrading.algorithm;

/**
 * 매도 유형
 */
public enum AskReason {
    /**
     * 매도 시간 경과
     */
    TIME,
    /**
     * 손절 매도
     */
    LOSS,
    /**
     * 익절 매도
     */
    GAIN,
    /**
     * 매매 주기에 매도하지 않음
     */
    SKIP,
    /**
     * 이평선 하락
     */
    MA_DOWN,
    /**
     * 분할 매도
     */
    SPLIT
}
