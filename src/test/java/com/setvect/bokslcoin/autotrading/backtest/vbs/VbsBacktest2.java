package com.setvect.bokslcoin.autotrading.backtest.vbs;

import com.setvect.bokslcoin.autotrading.util.DateRange;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 변동성 돌파 전략 백테스트
 */
public class VbsBacktest2 {
    @Test
    public void backtest() throws IOException {
        List<TestAnalysis> acc = new ArrayList<>();
        acc.add(test("상승이후 하락(2021-01-01, 2021-06-08), K=0.1, 투자비율: 50%", 0.1, 0.5, new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("상승이후 하락(2021-01-01, 2021-06-08), K=0.2, 투자비율: 50%", 0.2, 0.5, new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("상승이후 하락(2021-01-01, 2021-06-08), K=0.3, 투자비율: 50%", 0.3, 0.5, new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("상승이후 하락(2021-01-01, 2021-06-08), K=0.4, 투자비율: 50%", 0.4, 0.5, new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("상승이후 하락(2021-01-01, 2021-06-08), K=0.5, 투자비율: 50%", 0.5, 0.5, new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("상승이후 하락(2021-01-01, 2021-06-08), K=0.6, 투자비율: 50%", 0.6, 0.5, new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("상승이후 하락(2021-01-01, 2021-06-08), K=0.7, 투자비율: 50%", 0.7, 0.5, new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("상승이후 하락(2021-01-01, 2021-06-08), K=0.8, 투자비율: 50%", 0.8, 0.5, new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("상승이후 하락(2021-01-01, 2021-06-08), K=0.9, 투자비율: 50%", 0.9, 0.5, new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("상승이후 하락(2021-01-01, 2021-06-08), K=1.0, 투자비율: 50%", 1.0, 0.5, new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59")));

        acc.add(test("상승장(2020-11-01, 2021-04-14), K=0.1, 투자비율: 50%", 0.1, 0.5, new DateRange("2020-11-01T00:00:00", "2021-04-14T23:59:59")));
        acc.add(test("상승장(2020-11-01, 2021-04-14), K=0.2, 투자비율: 50%", 0.2, 0.5, new DateRange("2020-11-01T00:00:00", "2021-04-14T23:59:59")));
        acc.add(test("상승장(2020-11-01, 2021-04-14), K=0.3, 투자비율: 50%", 0.3, 0.5, new DateRange("2020-11-01T00:00:00", "2021-04-14T23:59:59")));
        acc.add(test("상승장(2020-11-01, 2021-04-14), K=0.4, 투자비율: 50%", 0.4, 0.5, new DateRange("2020-11-01T00:00:00", "2021-04-14T23:59:59")));
        acc.add(test("상승장(2020-11-01, 2021-04-14), K=0.5, 투자비율: 50%", 0.5, 0.5, new DateRange("2020-11-01T00:00:00", "2021-04-14T23:59:59")));
        acc.add(test("상승장(2020-11-01, 2021-04-14), K=0.6, 투자비율: 50%", 0.6, 0.5, new DateRange("2020-11-01T00:00:00", "2021-04-14T23:59:59")));
        acc.add(test("상승장(2020-11-01, 2021-04-14), K=0.7, 투자비율: 50%", 0.7, 0.5, new DateRange("2020-11-01T00:00:00", "2021-04-14T23:59:59")));
        acc.add(test("상승장(2020-11-01, 2021-04-14), K=0.8, 투자비율: 50%", 0.8, 0.5, new DateRange("2020-11-01T00:00:00", "2021-04-14T23:59:59")));
        acc.add(test("상승장(2020-11-01, 2021-04-14), K=0.9, 투자비율: 50%", 0.9, 0.5, new DateRange("2020-11-01T00:00:00", "2021-04-14T23:59:59")));
        acc.add(test("상승장(2020-11-01, 2021-04-14), K=1.0, 투자비율: 50%", 1.0, 0.5, new DateRange("2020-11-01T00:00:00", "2021-04-14T23:59:59")));

        acc.add(test("하락장(2021-04-14, 2021-06-08), K=0.1, 투자비율: 50%", 0.1, 0.5, new DateRange("2021-04-14T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("하락장(2021-04-14, 2021-06-08), K=0.2, 투자비율: 50%", 0.2, 0.5, new DateRange("2021-04-14T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("하락장(2021-04-14, 2021-06-08), K=0.3, 투자비율: 50%", 0.3, 0.5, new DateRange("2021-04-14T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("하락장(2021-04-14, 2021-06-08), K=0.4, 투자비율: 50%", 0.4, 0.5, new DateRange("2021-04-14T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("하락장(2021-04-14, 2021-06-08), K=0.5, 투자비율: 50%", 0.5, 0.5, new DateRange("2021-04-14T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("하락장(2021-04-14, 2021-06-08), K=0.6, 투자비율: 50%", 0.6, 0.5, new DateRange("2021-04-14T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("하락장(2021-04-14, 2021-06-08), K=0.7, 투자비율: 50%", 0.7, 0.5, new DateRange("2021-04-14T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("하락장(2021-04-14, 2021-06-08), K=0.8, 투자비율: 50%", 0.8, 0.5, new DateRange("2021-04-14T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("하락장(2021-04-14, 2021-06-08), K=0.9, 투자비율: 50%", 0.9, 0.5, new DateRange("2021-04-14T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("하락장(2021-04-14, 2021-06-08), K=1.0, 투자비율: 50%", 1.0, 0.5, new DateRange("2021-04-14T00:00:00", "2021-06-08T23:59:59")));

        acc.add(test("횡보장(2020-05-07, 2020-10-20), K=0.1, 투자비율: 50%", 0.1, 0.5, new DateRange("2020-05-07T00:00:00", "2020-10-20T23:59:59")));
        acc.add(test("횡보장(2020-05-07, 2020-10-20), K=0.2, 투자비율: 50%", 0.2, 0.5, new DateRange("2020-05-07T00:00:00", "2020-10-20T23:59:59")));
        acc.add(test("횡보장(2020-05-07, 2020-10-20), K=0.3, 투자비율: 50%", 0.3, 0.5, new DateRange("2020-05-07T00:00:00", "2020-10-20T23:59:59")));
        acc.add(test("횡보장(2020-05-07, 2020-10-20), K=0.4, 투자비율: 50%", 0.4, 0.5, new DateRange("2020-05-07T00:00:00", "2020-10-20T23:59:59")));
        acc.add(test("횡보장(2020-05-07, 2020-10-20), K=0.5, 투자비율: 50%", 0.5, 0.5, new DateRange("2020-05-07T00:00:00", "2020-10-20T23:59:59")));
        acc.add(test("횡보장(2020-05-07, 2020-10-20), K=0.6, 투자비율: 50%", 0.6, 0.5, new DateRange("2020-05-07T00:00:00", "2020-10-20T23:59:59")));
        acc.add(test("횡보장(2020-05-07, 2020-10-20), K=0.7, 투자비율: 50%", 0.7, 0.5, new DateRange("2020-05-07T00:00:00", "2020-10-20T23:59:59")));
        acc.add(test("횡보장(2020-05-07, 2020-10-20), K=0.8, 투자비율: 50%", 0.8, 0.5, new DateRange("2020-05-07T00:00:00", "2020-10-20T23:59:59")));
        acc.add(test("횡보장(2020-05-07, 2020-10-20), K=0.9, 투자비율: 50%", 0.9, 0.5, new DateRange("2020-05-07T00:00:00", "2020-10-20T23:59:59")));
        acc.add(test("횡보장(2020-05-07, 2020-10-20), K=1.0, 투자비율: 50%", 1.0, 0.5, new DateRange("2020-05-07T00:00:00", "2020-10-20T23:59:59")));

        acc.add(test("전체기간(2017-10-01, 2021-06-08), K=0.1, 투자비율: 50%", 0.1, 0.5, new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("전체기간(2017-10-01, 2021-06-08), K=0.2, 투자비율: 50%", 0.2, 0.5, new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("전체기간(2017-10-01, 2021-06-08), K=0.3, 투자비율: 50%", 0.3, 0.5, new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("전체기간(2017-10-01, 2021-06-08), K=0.4, 투자비율: 50%", 0.4, 0.5, new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("전체기간(2017-10-01, 2021-06-08), K=0.5, 투자비율: 50%", 0.5, 0.5, new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("전체기간(2017-10-01, 2021-06-08), K=0.6, 투자비율: 50%", 0.6, 0.5, new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("전체기간(2017-10-01, 2021-06-08), K=0.7, 투자비율: 50%", 0.7, 0.5, new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("전체기간(2017-10-01, 2021-06-08), K=0.8, 투자비율: 50%", 0.8, 0.5, new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("전체기간(2017-10-01, 2021-06-08), K=0.9, 투자비율: 50%", 0.9, 0.5, new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59")));
        acc.add(test("전체기간(2017-10-01, 2021-06-08), K=1.0, 투자비율: 50%", 1.0, 0.5, new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59")));


        String header = "설명,변동률,MDD,실현 수익,실현 MDD";
        StringBuffer report = new StringBuffer(header.replace(",", "\t") + "\n");
        for (TestAnalysis analysis : acc) {
            report.append(String.format("%s\t", analysis.getComment()));
            report.append(String.format("%,.2f%%\t", analysis.getCoinYield() * 100));
            report.append(String.format("%,.2f%%\t", analysis.getCoinMdd() * 100));
            report.append(String.format("%,.2f%%\t", analysis.getRealYield() * 100));
            report.append(String.format("%,.2f%%\n", analysis.getRealMdd() * 100));
        }

        File reportFile = new File("./backtest-result", "변동성돌파전략_백테스트_분석결과.txt");
        FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr");
        System.out.println("결과 파일:" + reportFile.getName());

        System.out.println("끝.");
    }

    private TestAnalysis test(String comment, double k, double rate, DateRange range) throws IOException {
        // === 1. 변수값 설정 ===
        VbsCondition condition = VbsCondition.builder()
                .k(k) // 변동성 돌파 판단 비율
                .rate(rate) // 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
                .range(range)// 분석 대상 기간
                .dataFile(new File("./craw-data/KRW-DOGE.json"))// 대상 코인
                .cash(10_000_000) // 최초 투자 금액
                .tradeMargin(0)// 매매시 채결 가격 차이
                .feeBid(0.0005) //  매수 수수료
                .feeAsk(0.0005)//  매도 수수료
                .build();

        // === 2. 백테스팅 ===
        List<VbsBacktestRow> testResult = VbsUtil.backtest(condition);
        TestAnalysis testAnalysis = VbsUtil.analysis(testResult);
        testAnalysis.setComment(comment);
        return testAnalysis;
    }


}
