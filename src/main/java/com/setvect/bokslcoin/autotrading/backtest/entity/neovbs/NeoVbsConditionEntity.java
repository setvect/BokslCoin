package com.setvect.bokslcoin.autotrading.backtest.entity.neovbs;

import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.entity.common.CommonConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.common.CommonTradeEntity;
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
@Entity(name = "XC_NEO_VBS_CONDITION")
@EntityListeners(AuditingEntityListener.class)
public class NeoVbsConditionEntity implements CommonConditionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "BACKTEST_CONDITION_SEQ")
    private int conditionSeq;

    @OneToMany(mappedBy = "conditionEntity")
    @OrderBy("tradeTimeKst ASC")
    @ToString.Exclude
    private List<NeoVbsTradeEntity> tradeEntityList;

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
     * 트레일링 스탑 진입점
     */
    @Column(name = "TRAILING_STOP_ENTER_RATE", nullable = false)
    private double trailingStopEnterRate;

    /**
     * 트레일링 스탑 손절
     * gainStopRate 이상 상승 후 전고점 대비 trailingStopRate 비율 만큼 하락하면 시장가 매도
     * 예를 들어 trailingStopRate 값이 0.02일 때 고점 수익률이 12%인 상태에서 10%미만으로 떨어지면 시장가 매도
     */
    @Column(name = "TRAILING_LOSS_STOP_RATE", nullable = false)
    private double trailingLossStopRate;

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

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CommonTradeEntity> List<T> getTradeEntityList() {
        // 타임 에러 안남
        return (List<T>) tradeEntityList;
    }
}
