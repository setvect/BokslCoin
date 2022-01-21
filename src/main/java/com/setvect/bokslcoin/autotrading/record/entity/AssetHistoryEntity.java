package com.setvect.bokslcoin.autotrading.record.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 자산 기록
 */
@Entity(name = "BA_ASSET_HISTORY")
@Table(indexes = {
        @Index(name = "IDX_REG_DATE_BA", columnList = "REG_DATE DESC"),
})
@Getter
@Setter
public class AssetHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ASSET_HISTORY_SEQ")
    private int assetHistorySeq;

    /**
     * 자산 종류
     * KRW, BTC, ETH, ...
     */
    @Column(name = "CURRENCY", length = 20, nullable = false)
    private String currency;

    /**
     * 투자금
     */
    @Column(name = "PRICE", nullable = false)
    private double price;

    /**
     * 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     */
    @Column(name = "YIELD")
    private Double yield;

    /**
     * 자산 조회 시간
     */
    @Column(name = "REG_DATE", nullable = false)
    private LocalDateTime regDate;
}
