package com.setvect.bokslcoin.autotrading.record.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity(name = "BA_ASSET_HISTORY")
@Getter
@Setter
public class AssetHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ASSET_HISTORY_SEQ", nullable = true)
    private int assetHistorySeq;

    /**
     * 자산 종류
     * KRW, BTC, ETH, ...
     */
    @Column(name = "CURRENCY", length = 20, nullable = false)
    private String currency;

    /**
     * 원화 환산 금액
     */
    @Column(name = "PRICE", nullable = false)
    private double price;

    /**
     * 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     */
    @Column(name = "YIELD", nullable = false)
    private double yield;

    /**
     * 자산 조회 시간
     */
    @Column(name = "REG_DATE", nullable = false)
    private LocalDateTime regDate;
}
