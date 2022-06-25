package com.setvect.bokslcoin.autotrading.backtest.mabs.service;

import com.setvect.bokslcoin.autotrading.backtest.common.BacktestHelper;
import com.setvect.bokslcoin.autotrading.backtest.common.BacktestHelperComponent;
import com.setvect.bokslcoin.autotrading.backtest.common.ExcelStyle;
import com.setvect.bokslcoin.autotrading.backtest.common.ReportMakerHelperService;
import com.setvect.bokslcoin.autotrading.backtest.common.model.AnalysisMultiCondition;
import com.setvect.bokslcoin.autotrading.backtest.common.model.CommonAnalysisReportResult;
import com.setvect.bokslcoin.autotrading.backtest.common.model.CommonTradeReportItem;
import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsTradeEntity;
import com.setvect.bokslcoin.autotrading.backtest.repository.MabsConditionEntityRepository;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MakeBacktestReportService {

    @Autowired
    private MabsConditionEntityRepository mabsConditionEntityRepository;
    @Autowired
    private BacktestHelperComponent backtestHelperService;

    /**
     * @param analysisMultiCondition 매매 조건
     * @return 멀티코인 매매 결과
     */
    @SneakyThrows
    @Transactional
    public CommonAnalysisReportResult<MabsConditionEntity, MabsTradeEntity> makeReport(AnalysisMultiCondition analysisMultiCondition) {
        List<MabsConditionEntity> mabsConditionEntityList = mabsConditionEntityRepository.findAllById(analysisMultiCondition.getConditionIdSet());

        // 대상코인의 수익률 정보를 제공
        List<CommonTradeReportItem<MabsTradeEntity>> tradeReportItems = BacktestHelper.trading(analysisMultiCondition, mabsConditionEntityList);

        CommonAnalysisReportResult<MabsConditionEntity, MabsTradeEntity> result = analysis(tradeReportItems, analysisMultiCondition);
        BacktestHelper.printSummary(result);
        makeReport(result);
        return result;
    }

    /**
     * @param tradeHistory   매매 내역
     * @param conditionMulti 조건
     * @return 분석결과
     */
    private CommonAnalysisReportResult<MabsConditionEntity, MabsTradeEntity> analysis(List<CommonTradeReportItem<MabsTradeEntity>> tradeHistory, AnalysisMultiCondition conditionMulti) {
        List<MabsConditionEntity> conditionByCoin = mabsConditionEntityRepository.findAllById(conditionMulti.getConditionIdSet());
        Set<String> markets = conditionByCoin.stream()
                .map(MabsConditionEntity::getMarket)
                .collect(Collectors.toSet());
        CommonAnalysisReportResult.MultiCoinHoldYield holdYield = backtestHelperService.calculateCoinHoldYield(conditionMulti.getRange(), markets);

        return CommonAnalysisReportResult.<MabsConditionEntity, MabsTradeEntity>builder()
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
    private void makeReport(CommonAnalysisReportResult<MabsConditionEntity, MabsTradeEntity> result) throws IOException {
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

    private XSSFSheet createReportSummary(CommonAnalysisReportResult<MabsConditionEntity, MabsTradeEntity> result, XSSFWorkbook workbook) {
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

        for (MabsConditionEntity mabsConditionEntity : result.getConditionList()) {
            report.append("\n---\n");
            report.append(String.format("조건아이디\t %s", mabsConditionEntity.getConditionSeq())).append("\n");
            report.append(String.format("분석주기\t %s", mabsConditionEntity.getTradePeriod())).append("\n");
            report.append(String.format("대상 코인\t %s", mabsConditionEntity.getMarket())).append("\n");
            report.append(String.format("상승 매수률\t %,.2f%%", mabsConditionEntity.getUpBuyRate() * 100)).append("\n");
            report.append(String.format("하락 매도률\t %,.2f%%", mabsConditionEntity.getDownSellRate() * 100)).append("\n");
            report.append(String.format("단기 이동평균 기간\t %d", mabsConditionEntity.getShortPeriod())).append("\n");
            report.append(String.format("장기 이동평균 기간\t %d", mabsConditionEntity.getLongPeriod())).append("\n");
            report.append(String.format("손절\t %,.2f%%", mabsConditionEntity.getLoseStopRate() * 100)).append("\n");
        }
        val sheet = workbook.createSheet();

        ReportMakerHelperService.textToSheet(report.toString(), sheet);
        sheet.setDefaultColumnWidth(60);
        return sheet;
    }


    private static XSSFSheet createTradeReport(CommonAnalysisReportResult<MabsConditionEntity, MabsTradeEntity> result, XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet();
        String header = "날짜(KST),날짜(UTC),코인,매매구분,단기 이동평균, 장기 이동평균,매수 체결 가격,최고수익률,최저수익률,매도 체결 가격,매도 이유,실현 수익률,매수금액,전체코인 매수금액,현금,수수료,투자 수익(수수료포함),투자 결과,현금 + 전체코인 매수금액 - 수수료,수익비";
        ReportMakerHelperService.applyHeader(sheet, header);
        int rowIdx = 1;

        XSSFCellStyle defaultStyle = ExcelStyle.createDefault(workbook);
        XSSFCellStyle dateTimeStyle = ExcelStyle.createDateTime(workbook);
        XSSFCellStyle commaStyle = ExcelStyle.createComma(workbook);
        XSSFCellStyle commaDecimalStyle = ExcelStyle.createCommaDecimal(workbook);
        XSSFCellStyle percentStyle = ExcelStyle.createPercent(workbook);
        XSSFCellStyle decimalStyle = ExcelStyle.createDecimal(workbook);

        for (CommonTradeReportItem<MabsTradeEntity> tradeItem : result.getTradeHistory()) {
            MabsTradeEntity mabsTradeEntity = tradeItem.getTradeEntity();
            MabsConditionEntity mabsConditionEntity = mabsTradeEntity.getConditionEntity();
            LocalDateTime tradeTimeKst = mabsTradeEntity.getTradeTimeKst();
            LocalDateTime utcTime = BacktestHelper.convertUtc(tradeTimeKst);

            XSSFRow row = sheet.createRow(rowIdx++);
            MabsTradeEntity tradeEntity = tradeItem.getTradeEntity();
            int cellIdx = 0;

            XSSFCell createCell = row.createCell(cellIdx++);
            createCell.setCellValue(tradeTimeKst);
            createCell.setCellStyle(dateTimeStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(utcTime);
            createCell.setCellStyle(dateTimeStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(mabsConditionEntity.getMarket());
            createCell.setCellStyle(defaultStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(mabsTradeEntity.getTradeType().name());
            createCell.setCellStyle(defaultStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(mabsTradeEntity.getMaShort());
            createCell.setCellStyle(commaStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(mabsTradeEntity.getMaLong());
            createCell.setCellStyle(commaStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(tradeItem.getBuyAmount());
            createCell.setCellStyle(commaStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(mabsTradeEntity.getHighYield());
            createCell.setCellStyle(percentStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(mabsTradeEntity.getLowYield());
            createCell.setCellStyle(percentStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(mabsTradeEntity.getUnitPrice());
            createCell.setCellStyle(commaStyle);

            createCell = row.createCell(cellIdx++);
            createCell.setCellValue(mabsTradeEntity.getSellReason() == null ? "" : mabsTradeEntity.getSellReason().name());
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
    public void makeReportMulti(List<CommonAnalysisReportResult<MabsConditionEntity, MabsTradeEntity>> accResult) {
        File reportFile = new File("./backtest-result", "이평선돌파_전략_백테스트_분석결과_" + Timestamp.valueOf(LocalDateTime.now()).getTime() + ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = ReportMakerHelperService.makeReportMultiList(accResult, workbook);
            workbook.setSheetName(workbook.getSheetIndex(sheet), "1. 평가표");

            XSSFSheet sheetCondition = ReportMakerHelperService.makeMultiCondition(accResult, workbook);
            workbook.setSheetName(workbook.getSheetIndex(sheetCondition), "2. 테스트 조건");

            try (FileOutputStream ous = new FileOutputStream(reportFile)) {
                workbook.write(ous);
                log.info("결과 파일:" + reportFile.getName());
            }
        }
    }
}
