package com.setvect.bokslcoin.autotrading.algorithm.neovbs;

import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 이평선 돌파 알고리즘 설정
 */
@Component
@ConfigurationProperties(prefix = "com.setvect.bokslcoin.autotrading.algorithm.neo-vbs-multi")
@Getter
@Setter
@ToString
public class NeoVbsMultiProperties {
    /**
     * 매수, 매도 대상 코인
     */
    private List<String> markets;
    /**
     * 최대 코인 매매 갯수
     */
    private int maxBuyCount;
    /**
     * 총 현금을 기준으로 투자 비율
     * 1은 100%, 0.5은 50% 투자
     */
    private double investRatio;
    /**
     * 손절 매도
     */
    private double loseStopRate;
    /**
     * 매매 주기
     */
    private PeriodType periodType;

    /**
     * 분석 캔들 시세 건수
     */
    private int periodCount;

    /**
     * 변동성 돌파 판단 비율
     */
    private double k;

    /**
     * 트레일링 스탑 진입점
     */
    private double gainStopRate;

    /**
     * gainStopRate 이상 상승 후 전고점 대비 trailingStopRate 비율 만큼 하락하면 시장가 매도
     * 예를 들어 trailingStopRate 값이 0.02일 때 고점 수익률이 12%인 상태에서 10%미만으로 떨어지면 시장가 매도
     */
    private double trailingStopRate;

}
