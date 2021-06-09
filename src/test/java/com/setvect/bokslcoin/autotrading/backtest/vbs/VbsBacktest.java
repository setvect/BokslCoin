package com.setvect.bokslcoin.autotrading.backtest.vbs;

import com.google.gson.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 변동성 돌파 전략 백테스트
 */
public class VbsBacktest {
    @Test
    public void backtest() throws IOException {
        // === 1. 변수값 설정 ===
        VbsCondition condition = VbsCondition.builder()
                .k(0.5) // 변동성 돌파 판단 비율
                .rate(0.5) // 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
                .range(new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59"))// 분석 대상 기간
                .dataFile(new File("./craw-data/KRW-BTC.json"))// 대상 코인
                .cash(10_000_000) // 최초 투자 금액
                .tradeMargin(1_000)// 매매시 채결 가격 차이
                .feeBid(0.0005) //  매수 수수료
                .feeAsk(0.0005)//  매도 수수료
                .build();

        // === 2. 백테스팅 ===
        List<BacktestRow> testResult = backtest(condition);
        TestAnalysis testAnalysis = analysis(testResult);

        System.out.printf("실제 수익: %,.2f%%\n", testAnalysis.getCoinYield() * 100);
        System.out.printf("실제 MDD: %,.2f%%\n", testAnalysis.getCoinMdd() * 100);
        System.out.printf("실현 수익: %,.2f%%\n", testAnalysis.getRealYield() * 100);
        System.out.printf("실현 MDD: %,.2f%%\n", testAnalysis.getRealMdd() * 100);

        // === 3. 리포트 ===
        makeReport(condition, testResult);

        System.out.println("끝.");
    }

    protected TestAnalysis analysis(List<BacktestRow> testResult) {
        TestAnalysis testAnalysis = new TestAnalysis();

        double coinYield = testResult.get(testResult.size() - 1).getCandleDay().getTradePrice() / testResult.get(0).getCandleDay().getOpeningPrice() - 1;
        testAnalysis.setCoinYield(coinYield);
        List<Double> values = new ArrayList<>();
        values.add(testResult.get(0).getCandleDay().getOpeningPrice());
        values.addAll(testResult.stream().skip(1).map(p -> p.getCandleDay().getTradePrice()).collect(Collectors.toList()));
        testAnalysis.setCoinMdd(ApplicationUtil.getMdd(values));

        values = new ArrayList<>();
        values.add(testResult.get(0).getFinalResult());
        values.addAll(testResult.stream().skip(1).map(p -> p.getFinalResult()).collect(Collectors.toList()));
        testAnalysis.setRealMdd(ApplicationUtil.getMdd(values));

        double realYield = testResult.get(testResult.size() - 1).getFinalResult() / testResult.get(0).getFinalResult() - 1;
        testAnalysis.setRealYield(realYield);
        return testAnalysis;
    }

    protected List<BacktestRow> backtest(VbsCondition condition) throws IOException {
        // 분석기간 코인 일봉 데이터
        List<CandleDay> candleDays = getAnalysisCandleDays(condition.getRange(), condition.getDataFile());
        List<BacktestRow> acc = new ArrayList<>();

        for (int i = 0; i < candleDays.size(); i++) {
            CandleDay candle = candleDays.get(i);
            BacktestRow row = new BacktestRow(candle);
            acc.add(row);
            if (i == 0) {
                row.setInvest(condition.getCash() * condition.getRate());
                row.setCash(condition.getCash() - row.getInvest());
                continue;
            }
            double targetPrice = getTargetValue(candleDays.get(i - 1), condition.getK());
            row.setTargetPrice(targetPrice);

            BacktestRow beforeRow = acc.get(i - 1);
            row.setInvest(beforeRow.getFinalResult() * condition.getRate());
            row.setCash(beforeRow.getFinalResult() - row.getInvest());
            if (targetPrice <= candle.getHighPrice()) {
                row.setTrade(true);
                row.setBidPrice(targetPrice + condition.getTradeMargin());
                row.setAskPrice(candle.getTradePrice() - condition.getTradeMargin());
                row.setFeePrice(row.getInvest() * condition.getFeeBid() + row.getInvestResult() * condition.getFeeAsk());
            }
        }
        return acc;
    }

    private void makeReport(VbsCondition condition, List<BacktestRow> acc) throws IOException {
        String header = "날짜,시가,고가,저가,종가,단위 수익률,매수 목표가,매매여부,매수 체결 가격,매도 체결 가격,실현 수익률,투자금,현금,투자 수익,수수료,투자 결과,현금 + 투자결과 - 수수료";
        StringBuffer report = new StringBuffer(header.replace(",", "\t") + "\n");
        for (BacktestRow row : acc) {
            String date = DateUtil.formatDateTime(row.getCandleDay().getCandleDateTimeUtc());
            report.append(String.format("%s\t", date));
            report.append(String.format("%,.0f\t", row.getCandleDay().getOpeningPrice()));
            report.append(String.format("%,.0f\t", row.getCandleDay().getHighPrice()));
            report.append(String.format("%,.0f\t", row.getCandleDay().getLowPrice()));
            report.append(String.format("%,.0f\t", row.getCandleDay().getTradePrice()));
            report.append(String.format("%,.2f%%\t", row.getCandleYield() * 100));
            report.append(String.format("%,.0f\t", row.getTargetPrice()));
            report.append(String.format("%s\t", row.isTrade()));
            report.append(String.format("%,.0f\t", row.getBidPrice()));
            report.append(String.format("%,.0f\t", row.getAskPrice()));
            report.append(String.format("%,.2f%%\t", row.getRealYield() * 100));
            report.append(String.format("%,.0f\t", row.getInvest()));
            report.append(String.format("%,.0f\t", row.getCash()));
            report.append(String.format("%,.0f\t", row.getGains()));
            report.append(String.format("%,.0f\t", row.getFeePrice()));
            report.append(String.format("%,.0f\t", row.getInvestResult()));
            report.append(String.format("%,.0f\n", row.getFinalResult()));
        }

        String reportFileName = String.format("%s(%s ~ %s).txt", FilenameUtils.getBaseName(condition.getDataFile().getName()), condition.getRange().getFromString(), condition.getRange().getToString());
        File reportFile = new File("./backtest-result", reportFileName);
        FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr");
        System.out.println("결과 파일:" + reportFile.getName());
    }

    private double getTargetValue(CandleDay candleDay, double k) {
        return candleDay.getTradePrice() + (candleDay.getHighPrice() - candleDay.getLowPrice()) * k;
    }

    private List<CandleDay> getAnalysisCandleDays(DateRange range, File dataFile) throws IOException {
        List<CandleDay> candles = GsonUtil.GSON.fromJson(FileUtils.readFileToString(dataFile, "utf-8"), new TypeToken<List<CandleDay>>() {
        }.getType());

        List<CandleDay> ca = candles.stream().filter(p -> range.isBetween(p.getCandleDateTimeUtc())).collect(Collectors.toList());
        // 과거 데이터를 먼저(날짜 기준 오름 차순 정렬)
        Collections.reverse(ca);
        return ca;
    }


}
