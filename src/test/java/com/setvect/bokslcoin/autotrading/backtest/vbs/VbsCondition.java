package com.setvect.bokslcoin.autotrading.backtest.vbs;

import com.setvect.bokslcoin.autotrading.util.DateRange;
import lombok.Builder;
import lombok.Getter;

import java.io.File;

/**
 * 변동성 돌파 전략 변수
 */
@Builder
@Getter
public class VbsCondition {
    // 변동성 돌파 판단 비율
    private final double k;
    // 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
    private final double rate;
    // 분석 대상 기간
    private final DateRange range;
    // 대상 코인
    private final File dataFile;
    // 최초 투자 금액
    private final double cash;
    // 매매시 채결 가격 차이
    // 시장가로 매매하기 때문에 한단계 낮거나 높은 호가로 매매가 되는 것을 고려함.
    // 매수 채결 가격 = 목표가격 + tradeMargin
    // 매도 채결 가격 = 종가 - tradeMargin
    private final double tradeMargin;
    //  매수 수수료
    private final double feeBid;
    //  매도 수수료
    private final double feeAsk;

}
