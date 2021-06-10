package com.setvect.bokslcoin.autotrading.backtest.vbs;

import com.setvect.bokslcoin.autotrading.util.DateRange;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 변동성 돌파 전략 백테스트
 */
public class VbsBacktest1 {
    @Test
    public void backtest() throws IOException {
        // === 1. 변수값 설정 ===
        VbsCondition condition = VbsCondition.builder()
                .k(0.5) // 변동성 돌파 판단 비율
                .rate(0.5) // 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
                .range(new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59"))// 분석 대상 기간
                .dataFile(new File("./craw-data/KRW-BTC.json"))// 대상 코인
                .cash(10_000_000) // 최초 투자 금액
                .tradeMargin(1_000)// 매매시 채결 가격 차이
                .feeBid(0.0005) //  매수 수수료
                .feeAsk(0.0005)//  매도 수수료
                .build();

        // === 2. 백테스팅 ===
        List<com.setvect.bokslcoin.autotrading.backtest.vbs.VbsBacktestRow> testResult = VbsUtil.backtest(condition);
        TestAnalysis testAnalysis = VbsUtil.analysis(testResult);

        System.out.printf("실제 수익: %,.2f%%\n", testAnalysis.getCoinYield() * 100);
        System.out.printf("실제 MDD: %,.2f%%\n", testAnalysis.getCoinMdd() * 100);
        System.out.printf("실현 수익: %,.2f%%\n", testAnalysis.getRealYield() * 100);
        System.out.printf("실현 MDD: %,.2f%%\n", testAnalysis.getRealMdd() * 100);

        // === 3. 리포트 ===
        VbsUtil.makeReport(condition, testResult);

        System.out.println("끝.");
    }


}
