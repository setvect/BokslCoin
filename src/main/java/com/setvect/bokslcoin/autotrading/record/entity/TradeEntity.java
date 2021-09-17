package com.setvect.bokslcoin.autotrading.record.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity(name = "AA_TRADE")
@Table(indexes = {
        @Index(name = "IDX_REG_DATE_AA", columnList = "REG_DATE DESC"),
})
@Getter
@Setter
public class TradeEntity {
    public enum TradeType {
        BUY, SELL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "TRADE_SEQ", nullable = true)
    private int tradeSeq;

    /**
     * 코인 이름
     * KRW-BTC, KRW-ETH, ...
     */
    @Column(name = "MARKET", length = 20, nullable = false)
    private String market;

    /**
     * 매수/매도
     */
    @Column(name = "TRADE_TYPE", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private TradeType tradeType;

    /**
     * 매도시 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     */
    @Column(name = "YIELD", nullable = true)
    private Double yield;


    /**
     * 거래 금액
     * 살때 현금 또는 팔고 나서 생기는 현금
     */
    @Column(name = "AMOUNT", nullable = false)
    private double amount;

    /**
     * 거래 단가
     */
    @Column(name = "UNIT_PRICE", nullable = false)
    private double unitPrice;

    /**
     * 거래 시간
     */
    @Column(name = "REG_DATE", nullable = false)
    private LocalDateTime regDate;

}
