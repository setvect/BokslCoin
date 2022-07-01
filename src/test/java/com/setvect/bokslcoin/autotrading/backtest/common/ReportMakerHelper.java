package com.setvect.bokslcoin.autotrading.backtest.common;

import com.setvect.bokslcoin.autotrading.backtest.common.model.AnalysisMultiCondition;
import com.setvect.bokslcoin.autotrading.backtest.common.model.CommonAnalysisReportResult;
import com.setvect.bokslcoin.autotrading.backtest.entity.common.CommonConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.common.CommonTradeEntity;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 리포트 생성에 필요한 공통 메소드 제공
 */
@Service
public class ReportMakerHelper {

    /**
     * @return 각 매매 결과
     */
    @NotNull
    public static XSSFSheet makeReportMultiList(List<? extends CommonAnalysisReportResult<? extends CommonConditionEntity, ? extends CommonTradeEntity>> accResult, XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet();
        String header = "분석기간,분석 아이디,대상 코인,투자비율,최초 투자금액,매수 수수료,매도 수수료,조건 설명," +
                "매수 후 보유 수익,매수 후 보유 MDD,실현 수익,실현 MDD,CAGR,매매 횟수,승률";
        ReportMakerHelper.applyHeader(sheet, header);
        int rowIdx = 1;

        XSSFCellStyle defaultStyle = ExcelStyle.createDefault(workbook);
        XSSFCellStyle commaStyle = ExcelStyle.createComma(workbook);
        XSSFCellStyle percentStyle = ExcelStyle.createPercent(workbook);
        XSSFCellStyle percentImportantStyle = ExcelStyle.createPercent(workbook);
        percentImportantStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        percentImportantStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.index);

        for (CommonAnalysisReportResult<? extends CommonConditionEntity, ? extends CommonTradeEntity> result : accResult) {
            AnalysisMultiCondition multiCondition = result.getCondition();

            XSSFRow row = sheet.createRow(rowIdx++);
            int cellIdx = 0;

            XSSFCell createCell = row.createCell(cellIdx++);
            createCell.setCellValue(multiCondition.getRange().toString());
            createCell.setCellStyle(defaultStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(StringUtils.join(result.getMabsConditionIds(), ", "));
            createCell.setCellStyle(defaultStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(StringUtils.join(result.getMarkets(), ", "));
            createCell.setCellStyle(defaultStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(multiCondition.getInvestRatio());
            createCell.setCellStyle(percentStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(multiCondition.getCash());
            createCell.setCellStyle(commaStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(multiCondition.getFeeBuy());
            createCell.setCellStyle(percentStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(multiCondition.getFeeSell());
            createCell.setCellStyle(percentStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(multiCondition.getComment());
            createCell.setCellStyle(defaultStyle);

            CommonAnalysisReportResult.MultiCoinHoldYield multiCoinHoldYield = result.getMultiCoinHoldYield();
            CommonAnalysisReportResult.YieldMdd sumYield = multiCoinHoldYield.getSumYield();

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(sumYield.getYield());
            createCell.setCellStyle(percentStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(sumYield.getMdd());
            createCell.setCellStyle(percentStyle);

            CommonAnalysisReportResult.TotalYield totalYield = result.getTotalYield();

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(totalYield.getYield());
            createCell.setCellStyle(percentImportantStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(totalYield.getMdd());
            createCell.setCellStyle(percentImportantStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(totalYield.getCagr());
            createCell.setCellStyle(percentImportantStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(totalYield.getTradeCount());
            createCell.setCellStyle(percentImportantStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(totalYield.getWinRate());
            createCell.setCellStyle(percentImportantStyle);

        }
        sheet.createFreezePane(0, 1);
        sheet.setDefaultColumnWidth(14);

        ExcelStyle.applyAllBorder(sheet);
        ExcelStyle.applyDefaultFont(sheet);

        return sheet;
    }


    public static void applyHeader(XSSFSheet sheet, String header) {
        List<String> headerList = Arrays.asList(header.split(","));
        XSSFRow rowHeader = sheet.createRow(0);
        for (int cellIdx = 0; cellIdx < headerList.size(); cellIdx++) {
            XSSFCell cell = rowHeader.createCell(cellIdx);
            cell.setCellValue(headerList.get(cellIdx));
            cell.setCellStyle(ExcelStyle.createHeaderRow(sheet.getWorkbook()));
        }
    }

    public static void textToSheet(String summary, XSSFSheet sheet) {
        String[] lines = summary.split("\n");
        sheet.createRow(sheet.getPhysicalNumberOfRows());

        for (String line : lines) {
            XSSFRow row = sheet.createRow(sheet.getPhysicalNumberOfRows());
            String[] columns = line.split("\t");

            int colIdx = 0;
            for (String colVal : columns) {
                XSSFCell cell = row.createCell(colIdx++);
                cell.setCellValue(colVal);
                cell.setCellStyle(ExcelStyle.createDefault(sheet.getWorkbook()));
            }
        }
        sheet.setDefaultColumnWidth(60);
    }

    public static String makeCommonSummary(CommonAnalysisReportResult<? extends CommonConditionEntity, ? extends CommonTradeEntity> result) {
        StringBuilder report = new StringBuilder();
        report.append("\n-----------\n");
        CommonAnalysisReportResult.MultiCoinHoldYield multiCoinHoldYield = result.getMultiCoinHoldYield();
        for (Map.Entry<String, CommonAnalysisReportResult.YieldMdd> coinHoldYield : multiCoinHoldYield.getCoinByYield().entrySet()) {
            String market = coinHoldYield.getKey();
            CommonAnalysisReportResult.YieldMdd coinYield = coinHoldYield.getValue();
            report.append(String.format("[%s] 실제 수익\t %,.2f%%", market, coinYield.getYield() * 100)).append("\n");
            report.append(String.format("[%s] 실제 MDD\t %,.2f%%", market, coinYield.getMdd() * 100)).append("\n");
        }
        CommonAnalysisReportResult.YieldMdd sumYield = multiCoinHoldYield.getSumYield();
        report.append(String.format("동일비중 수익\t %,.2f%%", sumYield.getYield() * 100)).append("\n");
        report.append(String.format("동일비중 MDD\t %,.2f%%", sumYield.getMdd() * 100)).append("\n");

        report.append("\n-----------\n");

        for (Map.Entry<String, CommonAnalysisReportResult.WinningRate> entry : result.getCoinWinningRate().entrySet()) {
            String market = entry.getKey();
            CommonAnalysisReportResult.WinningRate coinInvestment = entry.getValue();
            report.append(String.format("[%s] 수익금액 합계\t %,.0f", market, coinInvestment.getInvest())).append("\n");
            report.append(String.format("[%s] 매매 횟수\t %d", market, coinInvestment.getTradeCount())).append("\n");
            report.append(String.format("[%s] 승률\t %,.2f%%", market, coinInvestment.getWinRate() * 100)).append("\n");

        }
        report.append("\n-----------\n");
        CommonAnalysisReportResult.TotalYield totalYield = result.getTotalYield();
        report.append(String.format("실현 수익\t %,.2f%%", totalYield.getYield() * 100)).append("\n");
        report.append(String.format("실현 MDD\t %,.2f%%", totalYield.getMdd() * 100)).append("\n");
        report.append(String.format("매매회수\t %d", totalYield.getTradeCount())).append("\n");
        report.append(String.format("승률\t %,.2f%%", totalYield.getWinRate() * 100)).append("\n");
        report.append(String.format("CAGR\t %,.2f%%", totalYield.getCagr() * 100)).append("\n");

        report.append("\n-----------\n");

        AnalysisMultiCondition condition = result.getCondition();
        DateRange range = condition.getRange();
        report.append(String.format("분석기간\t %s", range)).append("\n");
        report.append(String.format("투자비율\t %,.2f%%", condition.getInvestRatio() * 100)).append("\n");
        report.append(String.format("최초 투자금액\t %,f", condition.getCash())).append("\n");
        report.append(String.format("매수 수수료\t %,.2f%%", condition.getFeeBuy() * 100)).append("\n");
        report.append(String.format("매도 수수료\t %,.2f%%", condition.getFeeSell() * 100)).append("\n");
        return report.toString();
    }
}
