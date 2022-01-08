package com.setvect.bokslcoin.autotrading.backtest.entity;

import com.setvect.bokslcoin.autotrading.algorithm.TradePeriod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 이평선 돌파 백테스트 조건
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "XA_MABS_CONDITION")
@EntityListeners(AuditingEntityListener.class)
@ToString(exclude = {"mabsTradeEntityList"})
public class MabsConditionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "BACKTEST_CONDITION_SEQ")
    private int mabsConditionSeq;

    @OneToMany(mappedBy = "mabsConditionEntity")
    @OrderBy("tradeTimeKst ASC")
    private List<MabsTradeEntity> mabsTradeEntityList;

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
    private TradePeriod tradePeriod;

    /**
     * 상승 매수률
     */
    @Column(name = "UP_BUY_RATE", nullable = false)
    private double upBuyRate;

    /**
     * 하락 매도률
     */
    @Column(name = "DOWN_BUY_RATE", nullable = false)
    private double downSellRate;

    /**
     * 단기 이동평균 기간
     */
    @Column(name = "SHORT_PERIOD", nullable = false)
    private int shortPeriod;

    /**
     * 장기 이동평균 기간
     */
    @Column(name = "LONG_PERIOD", nullable = false)
    private int longPeriod;
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
