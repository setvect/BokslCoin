package com.setvect.bokslcoin.autotrading.backtest.vbs;

import com.google.gson.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.backtest.TestAnalysis;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 변동성 돌파전략 백테스트
 */
public class VbsUtil {
    public static TestAnalysis analysis(List<VbsBacktestRow> testResult) {
        TestAnalysis testAnalysis = new TestAnalysis();
        if (testResult.isEmpty()) {
            return testAnalysis;
        }

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

    protected static List<VbsBacktestRow> backtest(VbsCondition condition) throws IOException {
        // 분석기간 코인 일봉 데이터
        List<CandleDay> candleDays = getAnalysisCandleDays(condition.getRange(), condition.getDataFile());
        List<VbsBacktestRow> acc = new ArrayList<>();

        for (int i = 0; i < candleDays.size(); i++) {
            CandleDay candle = candleDays.get(i);
            VbsBacktestRow row = new VbsBacktestRow(candle);
            acc.add(row);
            if (i == 0) {
                row.setInvest(condition.getCash() * condition.getRate());
                row.setCash(condition.getCash() - row.getInvest());
                continue;
            }
            double targetPrice = getTargetValue(candleDays.get(i - 1), condition.getK());
            row.setTargetPrice(targetPrice);

            VbsBacktestRow beforeRow = acc.get(i - 1);
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

    public static void makeReport(VbsCondition condition, List<VbsBacktestRow> acc) throws IOException {
        String header = "날짜,시가,고가,저가,종가,단위 수익률,매수 목표가,매매여부,매수 체결 가격,매도 체결 가격,실현 수익률,투자금,현금,투자 수익,수수료,투자 결과,현금 + 투자결과 - 수수료";
        StringBuffer report = new StringBuffer(header.replace(",", "\t") + "\n");
        for (VbsBacktestRow row : acc) {
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

    private static double getTargetValue(CandleDay candleDay, double k) {
        return candleDay.getTradePrice() + (candleDay.getHighPrice() - candleDay.getLowPrice()) * k;
    }

    private static List<CandleDay> getAnalysisCandleDays(DateRange range, File dataFile) throws IOException {
        List<CandleDay> candles = GsonUtil.GSON.fromJson(FileUtils.readFileToString(dataFile, "utf-8"), new TypeToken<List<CandleDay>>() {
        }.getType());

        List<CandleDay> ca = candles.stream().filter(p -> range.isBetween(p.getCandleDateTimeUtc())).collect(Collectors.toList());
        // 과거 데이터를 먼저(날짜 기준 오름 차순 정렬)
        Collections.reverse(ca);
        return ca;
    }

}
