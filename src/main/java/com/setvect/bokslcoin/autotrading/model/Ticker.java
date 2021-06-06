package com.setvect.bokslcoin.autotrading.model;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@ToString
/**
 * 현재가 정보
 */
public class Ticker {
    public enum Change {
        /**
         * 보함
         */
        EVEN,
        /**
         * 상승
         */
        RISE,
        /**
         * 하락
         */
        FAILL;
    }
    /**
     * 종목 구분 코드
     */
    private String market;
    /**
     * 최근 거래 일자(UTC)
     */
    private LocalDate tradeDate;
    /**
     * 최근 거래 시각(UTC)
     */
    private LocalTime tradeTime;
    /**
     * 최근 거래 일자(KST)
     */
    private LocalDate tradeDateKst;
    /**
     * 최근 거래 시각(KST)
     */
    private LocalTime tradeTimeKst;
    /**
     * 시가
     */
    private Double openingPrice;
    /**
     * 고가
     */
    private Double highPrice;
    /**
     * 저가
     */
    private Double lowPrice;
    /**
     * 종가
     */
    private Double tradePrice;
    /**
     * 전일 종가
     */
    private Double prevClosingPrice;
    /**
     * EVEN : 보합 RISE : 상승  FALL : 하락
     */
    private Change change;
    /**
     * 변화액의 절대값
     */
    private Double changePrice;
    /**
     * 변화율의 절대값
     */
    private Double changeRate;
    /**
     * 부호가 있는 변화액
     */
    private Double signedChangePrice;
    /**
     * 부호가 있는 변화율
     */
    private Double signedChangeRate;
    /**
     * 가장 최근 거래량
     */
    private Double tradeVolume;
    /**
     * 누적 거래대금(UTC 0시 기준)
     */
    private Double accTradePrice;
    /**
     * 24시간 누적 거래대금
     */
    private Double accTradePrice24h;
    /**
     * 누적 거래량(UTC 0시 기준)
     */
    private Double accTradeVolume;
    /**
     * 24시간 누적 거래량
     */
    private Double accTradeVolume24h;
    /**
     * 52주 신고가
     */
    private Double highest52WeekPrice;
    /**
     * 52주 신고가 달성일
     */
    private LocalDate highest52WeekDate;
    /**
     * 52주 신저가
     */
    private Double lowest52WeekPrice;
    /**
     * 52주 신저가 달성일
     */
    private LocalDate lowest52WeekDate;
    /**
     * 타임스탬프
     */
    private Long timestamp;
}
