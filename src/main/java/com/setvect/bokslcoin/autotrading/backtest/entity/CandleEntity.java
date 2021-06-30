package com.setvect.bokslcoin.autotrading.backtest.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 캔드 실세
 */
@Entity(name = "CANDLE")
@Table(indexes = {
        @Index(name = "IDX_MARKET", columnList = "MARKET"),
        @Index(name = "IDX_CANDLE_DATE_TIME_KST", columnList = "CANDLE_DATE_TIME_KST"),
        @Index(name = "IDX_CANDLE_DATE_TIME_UTC", columnList = "CANDLE_DATE_TIME_UTC"),
        @Index(name = "IDX_PERIOD_TYPE", columnList = "PERIOD_TYPE"),
})
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class CandleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "CANDLE_SEQ", nullable = true)
    private int candleSeq;

    @Column(name = "MARKET", length = 20, nullable = false)
    private String market;

    @Column(name = "CANDLE_DATE_TIME_UTC", nullable = false)
    private LocalDateTime candleDateTimeUtc;

    @Column(name = "CANDLE_DATE_TIME_KST", nullable = false)
    private LocalDateTime candleDateTimeKst;

    @Column(name = "PERIOD_TYPE", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PeriodType periodType;

    @Column(name = "OPENING_PRICE", nullable = false)
    private double openingPrice;

    @Column(name = "HIGH_PRICE", nullable = false)
    private double highPrice;

    @Column(name = "LOW_PRICE", nullable = false)
    private double lowPrice;

    @Column(name = "TRADE_PRICE", nullable = false)
    private double tradePrice;
}
