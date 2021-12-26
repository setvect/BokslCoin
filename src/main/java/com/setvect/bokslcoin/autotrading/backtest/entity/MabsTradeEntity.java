package com.setvect.bokslcoin.autotrading.backtest.entity;

import com.setvect.bokslcoin.autotrading.algorithm.AskReason;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 이평선 돌파 백테스트 매매 건별 정보
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "XB_MABS_TRADE")
@Table
public class MabsTradeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "TRADE_SEQ")
    private int tradeSeq;

    /**
     * 매매 조건 일련번호
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BACKTEST_CONDITION_SEQ")
    private MabsConditionEntity mabsConditionEntity;

    /**
     * 매수/매도
     */
    @Column(name = "TRADE_TYPE", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private TradeType tradeType;

    /**
     * 최고 수익률
     */
    @Column(name = "HIGH_YIELD")
    private double highYield;

    /**
     * 최저 수익률
     */
    @Column(name = "LOW_YIELD")
    private double lowYield;

    /**
     * 단기 이동평균 가격
     */
    @Column(name = "MA_SHORT", nullable = false)
    private double maShort;

    /**
     * 장기 이동평균 가격
     */
    @Column(name = "MA_LONG", nullable = false)
    private double maLong;

    /**
     * 매도시 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     * 매수는 0으로 표현
     */
    @Column(name = "YIELD")
    private double yield;

    /**
     * 거래 단가
     * - 매수 일경우 매수 단가
     * - 매도 일경우 매도 단가
     */
    @Column(name = "UNIT_PRICE", nullable = false)
    private double unitPrice;

    /**
     * 매도 이유
     */
    @Column(name = "SELL_TYPE", length = 20)
    @Enumerated(EnumType.STRING)
    private AskReason sellReason;

    /**
     * 거래시간(KST 기준)
     */
    @Column(name = "CANDLE_DATE_TIME_KST", nullable = false)
    private LocalDateTime candleDateTimeKst;
}
