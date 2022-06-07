package com.setvect.bokslcoin.autotrading.backtest.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Neo 변동성 돌파 전략
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "XC_VBS_CONDITION")
@EntityListeners(AuditingEntityListener.class)
@ToString(exclude = {"vbsTradeEntityList"})
public class NeoVbsConditionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "BACKTEST_CONDITION_SEQ")
    private int vbsConditionSeq;

    @OneToMany(mappedBy = "vbsConditionEntity")
    @OrderBy("tradeTimeKst ASC")
    @Setter
    private List<NeoVbsTradeEntity> neoVbsTradeEntityList;

    /**
     * 코인 이름
     * KRW-BTC, KRW-ETH, ...
     */
    @Column(name = "MARKET", length = 20, nullable = false)
    private String market;

    /**
     * 매매 주기
     */
    @Column(name = "TRADE_PERIOD", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PeriodType tradePeriod;

    /**
     * 변동율 K
     */
    @Column(name = "K", nullable = false)
    private double k;

    /**
     * 손절 손실률<br>
     * 예를 들어 0.05이면 수익률이 -5%가 되면 손절 매도
     */
    @Column(name = "LOSE_STOP_RATE", nullable = false)
    private double loseStopRate;

    /**
     * 조건에 대한 설명. 리포트에서 사용하기 위함
     */
    @Column(name = "COMMENT", length = 100)
    private String comment;

    /**
     * 백테스트 등록일
     */
    @Column(name = "REG_DATE", nullable = false)
    @CreatedDate
    private LocalDateTime regDate;
}
