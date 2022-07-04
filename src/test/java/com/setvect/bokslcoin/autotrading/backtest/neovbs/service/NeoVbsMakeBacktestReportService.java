package com.setvect.bokslcoin.autotrading.backtest.neovbs.service;

import com.setvect.bokslcoin.autotrading.backtest.common.BacktestHelper;
import com.setvect.bokslcoin.autotrading.backtest.common.BacktestHelperComponent;
import com.setvect.bokslcoin.autotrading.backtest.common.ExcelStyle;
import com.setvect.bokslcoin.autotrading.backtest.common.ReportMakerHelper;
import com.setvect.bokslcoin.autotrading.backtest.common.model.AnalysisMultiCondition;
import com.setvect.bokslcoin.autotrading.backtest.common.model.CommonAnalysisReportResult;
import com.setvect.bokslcoin.autotrading.backtest.common.model.CommonTradeReportItem;
import com.setvect.bokslcoin.autotrading.backtest.entity.neovbs.NeoVbsConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.neovbs.NeoVbsTradeEntity;
import com.setvect.bokslcoin.autotrading.backtest.repository.NeoVbsConditionEntityRepository;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NeoVbsMakeBacktestReportService {

    @Autowired
    private NeoVbsConditionEntityRepository neoVbsConditionEntityRepository;
    @Autowired
    private BacktestHelperComponent backtestHelperService;

    /**
     * @param analysisMultiCondition 매매 조건
     * @return 멀티코인 매매 결과
     */
    @SneakyThrows
    @Transactional
    public CommonAnalysisReportResult<NeoVbsConditionEntity, NeoVbsTradeEntity> makeReport(AnalysisMultiCondition analysisMultiCondition) {
        List<NeoVbsConditionEntity> neoVbsConditionEntityList = neoVbsConditionEntityRepository.findAllById(analysisMultiCondition.getConditionIdSet());

        // 대상코인의 수익률 정보를 제공
        List<CommonTradeReportItem<NeoVbsTradeEntity>> tradeReportItems = BacktestHelper.trading(analysisMultiCondition, neoVbsConditionEntityList);

        CommonAnalysisReportResult<NeoVbsConditionEntity, NeoVbsTradeEntity> result = analysis(tradeReportItems, analysisMultiCondition);
        BacktestHelper.printSummary(result);
        makeReport(result);
        return result;
    }

    /**
     * @param tradeHistory   매매 내역
     * @param conditionMulti 조건
     * @return 분석결과
     */
    private CommonAnalysisReportResult<NeoVbsConditionEntity, NeoVbsTradeEntity> analysis(List<CommonTradeReportItem<NeoVbsTradeEntity>> tradeHistory, AnalysisMultiCondition conditionMulti) {
        List<NeoVbsConditionEntity> conditionByCoin = neoVbsConditionEntityRepository.findAllById(conditionMulti.getConditionIdSet());
        Set<String> markets = conditionByCoin.stream()
                .map(NeoVbsConditionEntity::getMarket)
                .collect(Collectors.toSet());
        CommonAnalysisReportResult.MultiCoinHoldYield holdYield = backtestHelperService.calculateCoinHoldYield(conditionMulti.getRange(), markets);

        return CommonAnalysisReportResult.<NeoVbsConditionEntity, NeoVbsTradeEntity>builder()
                .condition(conditionMulti)
                .conditionList(conditionByCoin)
                .tradeHistory(tradeHistory)
                .multiCoinHoldYield(holdYield)
                .totalYield(BacktestHelper.calculateTotalYield(tradeHistory, conditionMulti))
                .coinWinningRate(BacktestHelper.calculateCoinInvestment(tradeHistory, conditionByCoin))
                .build();
    }

    /**
     * 분석결과 리포트 만듦
     *
     * @param result 분석결과
     * @throws IOException .
     */
    private void makeReport(CommonAnalysisReportResult<NeoVbsConditionEntity, NeoVbsTradeEntity> result) throws IOException {
        AnalysisMultiCondition condition = result.getCondition();
        DateRange range = condition.getRange();
        String coins = StringUtils.join(result.getMarkets(), ", ");
        String reportFileName = String.format("[%s~%s]_[%s].xlsx", range.getFromDateFormat(), range.getToDateFormat(), coins);
        File reportFile = new File("./backtest-result", reportFileName);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = createTradeReport(result, workbook);
            workbook.setSheetName(workbook.getSheetIndex(sheet), "1. 매매이력");

            sheet = createReportSummary(result, workbook);
            workbook.setSheetName(workbook.getSheetIndex(sheet), "5. 매매 요약결과 및 조건");

            try (FileOutputStream ous = new FileOutputStream(reportFile)) {
                workbook.write(ous);
                log.info("결과 파일:" + reportFile.getName());
            }
        }
    }

    private static XSSFSheet createReportSummary(
            CommonAnalysisReportResult<NeoVbsConditionEntity, NeoVbsTradeEntity> result,
            XSSFWorkbook workbook) {
        StringBuilder report = new StringBuilder();

        report.append(ReportMakerHelper.makeCommonSummary(result));

        for (NeoVbsConditionEntity neoVbsConditionEntity : result.getConditionList()) {
            report.append("\n---\n");
            report.append(String.format("조건아이디\t %s", neoVbsConditionEntity.getConditionSeq())).append("\n");
            report.append(String.format("분석주기\t %s", neoVbsConditionEntity.getTradePeriod())).append("\n");
            report.append(String.format("대상 코인\t %s", neoVbsConditionEntity.getMarket())).append("\n");
            report.append(String.format("K\t %,.2f%%", neoVbsConditionEntity.getK() * 100)).append("\n");
            report.append(String.format("트레일링 스탑 진입점\t %,.2f%%", neoVbsConditionEntity.getTrailingStopEnterRate() * 100)).append("\n");
            report.append(String.format("트레일링 스탑 손절\t %,.2f%%", neoVbsConditionEntity.getTrailingLossStopRate())).append("\n");
            report.append(String.format("손절\t %,.2f%%", neoVbsConditionEntity.getLoseStopRate() * 100)).append("\n");
        }
        XSSFSheet sheet = workbook.createSheet();

        ReportMakerHelper.textToSheet(report.toString(), sheet);
        return sheet;
    }

    private static XSSFSheet createTradeReport(CommonAnalysisReportResult<NeoVbsConditionEntity, NeoVbsTradeEntity> result, XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet();
        String header = "날짜(KST),날짜(UTC),코인,매매구분," +
                "매수 체결 가격,최고수익률,최저수익률,매도 체결 가격," +
                "매도 이유,실현 수익률,매수금액,전체코인 매수금액,현금," +
                "수수료,투자 수익(수수료포함),투자 결과,현금 + 전체코인 매수금액 - 수수료,수익비";
        ReportMakerHelper.applyHeader(sheet, header);
        int rowIdx = 1;

        XSSFCellStyle defaultStyle = ExcelStyle.createDefault(workbook);
        XSSFCellStyle dateTimeStyle = ExcelStyle.createDateTime(workbook);
        XSSFCellStyle commaStyle = ExcelStyle.createComma(workbook);
        XSSFCellStyle commaDecimalStyle = ExcelStyle.createCommaDecimal(workbook);
        XSSFCellStyle percentStyle = ExcelStyle.createPercent(workbook);
        XSSFCellStyle decimalStyle = ExcelStyle.createDecimal(workbook);

        for (CommonTradeReportItem<NeoVbsTradeEntity> tradeItem : result.getTradeHistory()) {
            NeoVbsTradeEntity neoVbsTradeEntity = tradeItem.getTradeEntity();
            NeoVbsConditionEntity neoVbsConditionEntity = neoVbsTradeEntity.getConditionEntity();
            LocalDateTime tradeTimeKst = neoVbsTradeEntity.getTradeTimeKst();
            LocalDateTime utcTime = BacktestHelper.convertUtc(tradeTimeKst);

            XSSFRow row = sheet.createRow(rowIdx++);
            NeoVbsTradeEntity tradeEntity = tradeItem.getTradeEntity();
            int cellIdx = 0;

            XSSFCell createCell = row.createCell(cellIdx++);
            createCell.setCellValue(tradeTimeKst);
            createCell.setCellStyle(dateTimeStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(utcTime);
            createCell.setCellStyle(dateTimeStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(neoVbsConditionEntity.getMarket());
            createCell.setCellStyle(defaultStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(neoVbsTradeEntity.getTradeType().name());
            createCell.setCellStyle(defaultStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(tradeItem.getBuyAmount());
            createCell.setCellStyle(commaStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(neoVbsTradeEntity.getHighYield());
            createCell.setCellStyle(percentStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(neoVbsTradeEntity.getLowYield());
            createCell.setCellStyle(percentStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(neoVbsTradeEntity.getUnitPrice());
            createCell.setCellStyle(commaStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(neoVbsTradeEntity.getSellReason() == null ? "" : neoVbsTradeEntity.getSellReason().name());
            createCell.setCellStyle(defaultStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(tradeItem.getRealYield());
            createCell.setCellStyle(percentStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(tradeItem.getBuyAmount());
            createCell.setCellStyle(commaStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(tradeItem.getBuyTotalAmount());
            createCell.setCellStyle(commaStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(tradeItem.getCash());
            createCell.setCellStyle(commaStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(tradeItem.getFeePrice());
            createCell.setCellStyle(commaDecimalStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(tradeItem.getGains());
            createCell.setCellStyle(commaStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(tradeItem.getInvestResult());
            createCell.setCellStyle(commaStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(tradeItem.getFinalResult());
            createCell.setCellStyle(commaStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(tradeItem.getFinalResult() / result.getCondition().getCash());
            createCell.setCellStyle(decimalStyle);
        }

        sheet.createFreezePane(0, 1);
        sheet.setDefaultColumnWidth(14);
        sheet.setColumnWidth(0, 5000);
        sheet.setColumnWidth(1, 5000);

        ExcelStyle.applyAllBorder(sheet);
        ExcelStyle.applyDefaultFont(sheet);

        return sheet;
    }


    /**
     * 복수개의 분석 결과 요약 리포트
     *
     * @param accResult 분석결과
     */
    @SneakyThrows
    public void makeReportMulti(List<CommonAnalysisReportResult<NeoVbsConditionEntity, NeoVbsTradeEntity>> accResult) {
        File reportFile = new File("./backtest-result", "이평선돌파_전략_백테스트_분석결과_" + Timestamp.valueOf(LocalDateTime.now()).getTime() + ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = ReportMakerHelper.makeReportMultiList(accResult, workbook);
            workbook.setSheetName(workbook.getSheetIndex(sheet), "1. 평가표");

            XSSFSheet sheetCondition = makeMultiCondition(accResult, workbook);
            workbook.setSheetName(workbook.getSheetIndex(sheetCondition), "2. 테스트 조건");

            try (FileOutputStream ous = new FileOutputStream(reportFile)) {
                workbook.write(ous);
                log.info("결과 파일:" + reportFile.getName());
            }
        }
    }


    private static XSSFSheet makeMultiCondition(List<CommonAnalysisReportResult<NeoVbsConditionEntity, NeoVbsTradeEntity>> accResult, XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet();
        String header = "조건 아이디,분석주기,대상 코인,K,트레일링 스탑 진입,트레일링 스탑 손절,손절률";
        ReportMakerHelper.applyHeader(sheet, header);
        int rowIdx = 1;

        List<NeoVbsConditionEntity> conditionAll = accResult.stream()
                .flatMap(p -> p.getConditionList().stream())
                .distinct()
                .sorted(Comparator.comparingInt(NeoVbsConditionEntity::getConditionSeq))
                .collect(Collectors.toList());

        XSSFCellStyle defaultStyle = ExcelStyle.createDefault(workbook);
        XSSFCellStyle percentStyle = ExcelStyle.createPercent(workbook);

        for (NeoVbsConditionEntity condition : conditionAll) {
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
            createCell.setCellValue(condition.getK());
            createCell.setCellStyle(percentStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(condition.getTrailingStopEnterRate());
            createCell.setCellStyle(percentStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(condition.getTrailingLossStopRate());
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
}
