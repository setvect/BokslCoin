package com.setvect.bokslcoin.autotrading.backtest.vbsstop;

import com.setvect.bokslcoin.autotrading.backtest.TestAnalysis;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VbsStopUtil {
    public static void makeReport(VbsStopCondition condition, List<VbsStopBacktestRow> tradeHistory, TestAnalysis testAnalysis) throws IOException {
        String header = "날짜(KST), 날짜(UTC),시가,고가,저가,종가,직전 종가,단위 수익률,매수 목표가,매매여부,매수 체결 가격,매도 체결 가격,매도 이유,실현 수익률,투자금,현금,투자 수익,수수료,투자 결과,현금 + 투자결과 - 수수료";
        StringBuffer report = new StringBuffer(header.replace(",", "\t") + "\n");
        for (VbsStopBacktestRow row : tradeHistory) {
            String dateKst = DateUtil.formatDateTime(row.getCandle().getCandleDateTimeKst());
            String dateUtc = DateUtil.formatDateTime(row.getCandle().getCandleDateTimeUtc());
            report.append(String.format("%s\t", dateKst));
            report.append(String.format("%s\t", dateUtc));
            report.append(String.format("%,.0f\t", row.getCandle().getOpeningPrice()));
            report.append(String.format("%,.0f\t", row.getCandle().getHighPrice()));
            report.append(String.format("%,.0f\t", row.getCandle().getLowPrice()));
            report.append(String.format("%,.0f\t", row.getCandle().getTradePrice()));
            report.append(String.format("%,.0f\t", row.getBeforeTradePrice()));
            report.append(String.format("%,.2f%%\t", row.getCandleYield() * 100));
            report.append(String.format("%,.0f\t", row.getTargetPrice()));
            report.append(String.format("%s\t", row.isTrade()));
            report.append(String.format("%,.0f\t", row.getBidPrice()));
            report.append(String.format("%,.0f\t", row.getAskPrice()));
            report.append(String.format("%s\t", row.getAskReason() == null ? "" : row.getAskReason()));
            report.append(String.format("%,.2f%%\t", row.getRealYield() * 100));
            report.append(String.format("%,.0f\t", row.getInvestmentAmount()));
            report.append(String.format("%,.0f\t", row.getCash()));
            report.append(String.format("%,.0f\t", row.getGains()));
            report.append(String.format("%,.0f\t", row.getFeePrice()));
            report.append(String.format("%,.0f\t", row.getInvestResult()));
            report.append(String.format("%,.0f\n", row.getFinalResult()));
        }

        String reportFileName = String.format("%s(%s ~ %s)_%s.txt",
                FilenameUtils.getBaseName(condition.getMarket()), condition.getRange().getFromString(), condition.getRange().getToString(), condition.getTradePeriod());
        report.append("\n\n-----------\n");
        report.append(String.format("실제 수익\t %,.2f%%", testAnalysis.getCoinYield() * 100) + "\n");
        report.append(String.format("실제 MDD\t %,.2f%%", testAnalysis.getCoinMdd() * 100) + "\n");
        report.append(String.format("실현 수익\t %,.2f%%", testAnalysis.getRealYield() * 100) + "\n");
        report.append(String.format("실현 MDD\t %,.2f%%", testAnalysis.getRealMdd() * 100) + "\n");
        report.append("\n\n-----------\n");
        report.append(String.format("변동성 비율\t %,.2f", condition.getK()) + "\n");
        report.append(String.format("투자비율\t %,.2f%%", condition.getInvestRatio() * 100) + "\n");
        report.append(String.format("분석기간\t %s", condition.getRange()) + "\n");
        report.append(String.format("대상 코인\t %s", condition.getMarket()) + "\n");
        report.append(String.format("최초 투자금액\t %,f", condition.getCash()) + "\n");
        report.append(String.format("매매 마진\t %,f", condition.getTradeMargin()) + "\n");
        report.append(String.format("매수 수수료\t %,.2f%%", condition.getFeeBid() * 100) + "\n");
        report.append(String.format("매도 수수료\t %,.2f%%", condition.getFeeAsk() * 100) + "\n");
        report.append(String.format("손절\t %,.2f%%", condition.getLoseStopRate() * 100) + "\n");
        report.append(String.format("익절\t %,.2f%%", condition.getGainStopRate() * 100) + "\n");
        report.append(String.format("분석주기\t %s", condition.getTradePeriod()) + "\n");


        File reportFile = new File("./backtest-result", reportFileName);
        FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr");
        System.out.println("결과 파일:" + reportFile.getName());
    }

    public static TestAnalysis analysis(List<VbsStopBacktestRow> tradeHistory) {
        TestAnalysis testAnalysis = new TestAnalysis();
        if (tradeHistory.isEmpty()) {
            return testAnalysis;
        }

        double coinYield = tradeHistory.get(tradeHistory.size() - 1).getCandle().getTradePrice() / tradeHistory.get(0).getCandle().getOpeningPrice() - 1;
        testAnalysis.setCoinYield(coinYield);
        List<Double> values = new ArrayList<>();
        values.add(tradeHistory.get(0).getCandle().getOpeningPrice());
        values.addAll(tradeHistory.stream().skip(1).map(p -> p.getCandle().getTradePrice()).collect(Collectors.toList()));
        testAnalysis.setCoinMdd(ApplicationUtil.getMdd(values));

        values = new ArrayList<>();
        values.add(tradeHistory.get(0).getFinalResult());
        values.addAll(tradeHistory.stream().skip(1).map(p -> p.getFinalResult()).collect(Collectors.toList()));
        testAnalysis.setRealMdd(ApplicationUtil.getMdd(values));

        double realYield = tradeHistory.get(tradeHistory.size() - 1).getFinalResult() / tradeHistory.get(0).getFinalResult() - 1;
        testAnalysis.setRealYield(realYield);
        return testAnalysis;
    }
}
