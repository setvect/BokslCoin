package com.setvect.bokslcoin.autotrading.algorithm.mabs;

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
@ConfigurationProperties(prefix = "com.setvect.bokslcoin.autotrading.algorithm")
@Getter
@Setter
@ToString
public class MabsMultiProperties {
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
     * 상승 매수률
     */
    private double upBuyRate;
    /**
     * 하락 매도률
     */
    private double downSellRate;
    /**
     * 단기 이동평균 기간
     */
    private int shortPeriod;
    /**
     * 장기 이동평균 기간
     */
    private int longPeriod;
    /**
     * 프로그램을 시작하자마자 매수하는걸 방지하기 위함.
     * true: 직전 이동평균을 감지해 새롭게 돌파 했을 때만 매수
     * false: 프로그램 시작과 동시에 매수 조건이 만족하면 매수, 고가에 매수할 가능성 있음
     */
    private boolean newMasBuy;
}
