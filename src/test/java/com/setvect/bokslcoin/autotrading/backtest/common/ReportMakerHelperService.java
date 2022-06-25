package com.setvect.bokslcoin.autotrading.backtest.common;

import com.setvect.bokslcoin.autotrading.backtest.common.model.AnalysisMultiCondition;
import com.setvect.bokslcoin.autotrading.backtest.common.model.CommonAnalysisReportResult;
import com.setvect.bokslcoin.autotrading.backtest.entity.common.CommonConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.common.CommonTradeEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsTradeEntity;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 리포트 생성에 필요한 공통 메소드 제공
 */
// TODO 리포트 관련 메소드 이곳으로 옮기기
@Service
public class ReportMakerHelperService {

    /**
     * @return 각 매매 결과
     */
    @NotNull
    public static XSSFSheet makeReportMultiList(List<? extends CommonAnalysisReportResult<? extends CommonConditionEntity, ? extends CommonTradeEntity>> accResult, XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet();
        String header = "분석기간,분석 아이디,대상 코인,투자비율,최초 투자금액,매수 수수료,매도 수수료,조건 설명," +
                "매수 후 보유 수익,매수 후 보유 MDD,실현 수익,실현 MDD,CAGR,매매 횟수,승률";
        ReportMakerHelperService.applyHeader(sheet, header);
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

    public static XSSFSheet makeMultiCondition(List<CommonAnalysisReportResult<MabsConditionEntity, MabsTradeEntity>> accResult, XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet();
        String header = "조건 아이디,분석주기,대상 코인,상승 매수률,하락 매도률,단기 이동평균,장기 이동평균,손절률";
        ReportMakerHelperService.applyHeader(sheet, header);
        int rowIdx = 1;

        List<MabsConditionEntity> conditionAll = accResult.stream()
                .flatMap(p -> p.getConditionList().stream())
                .distinct()
                .sorted(Comparator.comparingInt(MabsConditionEntity::getConditionSeq))
                .collect(Collectors.toList());

        XSSFCellStyle defaultStyle = ExcelStyle.createDefault(workbook);
        XSSFCellStyle percentStyle = ExcelStyle.createPercent(workbook);

        for (MabsConditionEntity condition : conditionAll) {

            XSSFRow row = sheet.createRow(rowIdx++);
            int cellIdx = 0;

            XSSFCell createCell = row.createCell(cellIdx++);
            createCell.setCellValue(condition.getConditionSeq());
            createCell.setCellStyle(defaultStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(condition.getTradePeriod().toString());
            createCell.setCellStyle(defaultStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(condition.getMarket());
            createCell.setCellStyle(defaultStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(condition.getUpBuyRate());
            createCell.setCellStyle(percentStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(condition.getDownSellRate());
            createCell.setCellStyle(percentStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(condition.getShortPeriod());
            createCell.setCellStyle(defaultStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(condition.getLongPeriod());
            createCell.setCellStyle(defaultStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(condition.getLoseStopRate());
            createCell.setCellStyle(percentStyle);
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
            val row = sheet.createRow(sheet.getPhysicalNumberOfRows());
            val columns = line.split("\t");

            int colIdx = 0;
            for (String colVal : columns) {
                val cell = row.createCell(colIdx++);
                cell.setCellValue(colVal);
                cell.setCellStyle(ExcelStyle.createDefault(sheet.getWorkbook()));
            }
        }
    }


}
