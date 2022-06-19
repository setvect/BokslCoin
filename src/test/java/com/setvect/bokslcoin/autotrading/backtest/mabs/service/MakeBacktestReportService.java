package com.setvect.bokslcoin.autotrading.backtest.mabs.service;

import com.setvect.bokslcoin.autotrading.backtest.common.BacktestHelper;
import com.setvect.bokslcoin.autotrading.backtest.common.BacktestHelperComponent;
import com.setvect.bokslcoin.autotrading.backtest.common.model.AnalysisMultiCondition;
import com.setvect.bokslcoin.autotrading.backtest.common.model.CommonAnalysisReportResult;
import com.setvect.bokslcoin.autotrading.backtest.common.model.CommonTradeReportItem;
import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsTradeEntity;
import com.setvect.bokslcoin.autotrading.backtest.repository.MabsConditionEntityRepository;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
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
        List<CommonTradeReportItem<MabsTradeEntity>> tradeReportItems = trading(analysisMultiCondition);
        CommonAnalysisReportResult<MabsConditionEntity, MabsTradeEntity> result = analysis(tradeReportItems, analysisMultiCondition);
        BacktestHelper.printSummary(result);
        makeReport(result);
        return result;
    }

    /**
     * @param analysisMultiCondition 매매 분석 조건
     * @return 대상코인의 수익률 정보를 제공
     */
    private List<CommonTradeReportItem<MabsTradeEntity>> trading(AnalysisMultiCondition analysisMultiCondition) throws RuntimeException {
        List<MabsConditionEntity> mabsConditionEntityList = mabsConditionEntityRepository.findAllById(analysisMultiCondition.getConditionIdSet());
        List<List<MabsTradeEntity>> tradeList = mabsConditionEntityList.stream().map(MabsConditionEntity::getTradeEntityList).collect(Collectors.toList());

        List<MabsTradeEntity> allTrade = new ArrayList<>();
        for (List<MabsTradeEntity> tradeHistory : tradeList) {
            List<MabsTradeEntity> targetTradeHistory = tradeHistory.stream()
                    .filter(p -> analysisMultiCondition.getRange().isBetween(p.getTradeTimeKst()))
                    .collect(Collectors.toList());
            if (targetTradeHistory.isEmpty()) {
                continue;
            }

            List<MabsTradeEntity> list = makePairTrade(targetTradeHistory);
            allTrade.addAll(list);
        }
        allTrade.sort(Comparator.comparing(MabsTradeEntity::getTradeTimeKst));

        List<CommonTradeReportItem<MabsTradeEntity>> reportHistory = new ArrayList<>();

        Set<Integer> ids = analysisMultiCondition.getConditionIdSet();
        int allowBuyCount = ids.size();

        int buyCount = 0;
        double cash = analysisMultiCondition.getCash();

        // 코인명:매수가격
        Map<String, Double> buyCoinAmount = new HashMap<>();

        // TODO 공통 코드로 만들수 있을 것 같음
        for (MabsTradeEntity trade : allTrade) {
            CommonTradeReportItem.CommonTradeReportItemBuilder<MabsTradeEntity> itemBuilder = CommonTradeReportItem.builder();
            itemBuilder.tradeEntity(trade);
            String market = trade.getConditionEntity().getMarket();

            if (trade.getTradeType() == TradeType.BUY) {
                if (allowBuyCount <= buyCount) {
                    throw new RuntimeException(String.format("매수 종목 한도 초과. 종모 매수 한도: %,d", allowBuyCount));
                }
                int rate = allowBuyCount - buyCount;
                double buyAmount = (cash * analysisMultiCondition.getInvestRatio()) / rate;
                double feePrice = buyAmount * analysisMultiCondition.getFeeBuy();

                if (buyCoinAmount.containsKey(market)) {
                    throw new RuntimeException(String.format("이미 매수한 코인 입니다. 코인명: %s", market));
                }

                buyCoinAmount.put(market, buyAmount);

                cash -= buyAmount;
                double totalBuyAmount = BacktestHelper.getBuyCoin(buyCoinAmount);
                itemBuilder.buyAmount(buyAmount);
                itemBuilder.buyTotalAmount(totalBuyAmount);
                itemBuilder.feePrice(feePrice);
                itemBuilder.cash(cash);
                buyCount++;
            } else if (trade.getTradeType() == TradeType.SELL) {
                if (buyCount <= 0) {
                    throw new RuntimeException("매도할 종목이 없습니다.");
                }
                if (!buyCoinAmount.containsKey(market)) {
                    throw new RuntimeException(String.format("매수 내역이 없습니다. 코인명: %s", market));
                }

                double buyAmount = buyCoinAmount.get(market);
                buyCoinAmount.remove(market);
                double gains = buyAmount * trade.getYield();
                double sellAmount = buyAmount + gains;
                double feePrice = sellAmount * analysisMultiCondition.getFeeBuy();
                gains -= feePrice;
                cash += buyAmount + gains;

                double totalBuyAmount = BacktestHelper.getBuyCoin(buyCoinAmount);
                itemBuilder.buyAmount(buyAmount);
                itemBuilder.buyTotalAmount(totalBuyAmount);
                itemBuilder.cash(cash);
                itemBuilder.feePrice(feePrice);
                itemBuilder.gains(gains);

                buyCount--;
            }
            reportHistory.add(itemBuilder.build());
        }
        return reportHistory;
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
     * 첫 거래는 매수로 시작하고 마지막 거래는 매도로 끝나야됨
     *
     * @param targetTradeHistory 거래 내역
     * @return 첫 거래는 매수, 마지막 거래는 매도로 끝나는 거래 내역
     */
    private List<MabsTradeEntity> makePairTrade(List<MabsTradeEntity> targetTradeHistory) {
        int skip = targetTradeHistory.get(0).getTradeType() == TradeType.BUY ? 0 : 1;
        int size = targetTradeHistory.size();
        int limit = targetTradeHistory.get(size - 1).getTradeType() == TradeType.SELL ? size - skip : size - skip - 1;
        return targetTradeHistory.stream()
                .skip(skip)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 분석결과 리포트 만듦
     *
     * @param result 분석결과
     * @throws IOException .
     */
    private void makeReport(CommonAnalysisReportResult<MabsConditionEntity, MabsTradeEntity> result) throws IOException {
        String header = "날짜(KST),날짜(UTC),코인,매매구분,단기 이동평균, 장기 이동평균,매수 체결 가격,최고수익률,최저수익률,매도 체결 가격,매도 이유,실현 수익률,매수금액,전체코인 매수금액,현금,수수료,투자 수익(수수료포함),투자 결과,현금 + 전체코인 매수금액 - 수수료,수익비";
        StringBuilder report = new StringBuilder(header.replace(",", "\t")).append("\n");

        for (CommonTradeReportItem<MabsTradeEntity> row : result.getTradeHistory()) {
            MabsTradeEntity mabsTradeEntity = row.getTradeEntity();
            MabsConditionEntity mabsConditionEntity = mabsTradeEntity.getConditionEntity();
            LocalDateTime tradeTimeKst = mabsTradeEntity.getTradeTimeKst();
            String dateKst = DateUtil.formatDateTime(tradeTimeKst);
            LocalDateTime utcTime = BacktestHelper.convertUtc(tradeTimeKst);
            String dateUtc = DateUtil.formatDateTime(utcTime);

            report.append(String.format("%s\t", dateKst));
            report.append(String.format("%s\t", dateUtc));
            report.append(String.format("%s\t", mabsConditionEntity.getMarket()));
            report.append(String.format("%s\t", mabsTradeEntity.getTradeType()));
            report.append(String.format("%,.0f\t", mabsTradeEntity.getMaShort()));
            report.append(String.format("%,.0f\t", mabsTradeEntity.getMaLong()));
            report.append(String.format("%,.0f\t", row.getBuyAmount()));
            report.append(String.format("%,.2f%%\t", mabsTradeEntity.getHighYield() * 100));
            report.append(String.format("%,.2f%%\t", mabsTradeEntity.getLowYield() * 100));
            report.append(String.format("%,.0f\t", mabsTradeEntity.getUnitPrice()));
            report.append(String.format("%s\t", mabsTradeEntity.getSellReason() == null ? "" : mabsTradeEntity.getSellReason()));
            report.append(String.format("%,.2f%%\t", row.getRealYield() * 100));
            report.append(String.format("%,.0f\t", row.getBuyAmount()));
            report.append(String.format("%,.0f\t", row.getBuyTotalAmount()));
            report.append(String.format("%,.0f\t", row.getCash()));
            report.append(String.format("%,.0f\t", row.getFeePrice()));
            report.append(String.format("%,.0f\t", row.getGains()));
            report.append(String.format("%,.0f\t", row.getInvestResult()));
            report.append(String.format("%,.0f\t", row.getFinalResult()));
            report.append(String.format("%,.2f\n", row.getFinalResult() / result.getCondition().getCash()));
        }

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

        String coins = StringUtils.join(result.getMarkets(), ", ");
        String reportFileName = String.format("[%s~%s]_[%s].txt", range.getFromDateFormat(), range.getToDateFormat(), coins);

        File reportFile = new File("./backtest-result", reportFileName);
        FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr");
        System.out.println("결과 파일:" + reportFile.getName());
    }

    /**
     * 복수개의 분석 결과 요약 리포트
     *
     * @param accResult 분석결과
     */
    @SneakyThrows
    public void makeReportMulti(List<CommonAnalysisReportResult<MabsConditionEntity, MabsTradeEntity>> accResult) {
        String resultList = BacktestHelper.makeReportMultiList(accResult);

        StringBuilder report = new StringBuilder(resultList);
        // 사용한 매매 조건 정보
        report.append("\n----------------\n");
        report.append("조건들\n");
        String condHeader = "조건 아이디,분석주기,대상 코인,상승 매수률,하락 매도률,단기 이동평균,장기 이동평균,손절률";
        report.append(condHeader.replace(",", "\t")).append("\n");
        List<MabsConditionEntity> conditionAll = accResult.stream()
                .flatMap(p -> p.getConditionList().stream())
                .distinct()
                .sorted(Comparator.comparingInt(MabsConditionEntity::getConditionSeq))
                .collect(Collectors.toList());

        for (MabsConditionEntity condition : conditionAll) {
            String reportRow = String.format("%s\t", condition.getConditionSeq()) +
                    String.format("%s\t", condition.getTradePeriod()) +
                    String.format("%s\t", condition.getMarket()) +
                    String.format("%,.2f%%\t", condition.getUpBuyRate() * 100) +
                    String.format("%,.2f%%\t", condition.getDownSellRate() * 100) +
                    String.format("%d\t", condition.getShortPeriod()) +
                    String.format("%d\t", condition.getLongPeriod()) +
                    String.format("%,.2f%%\t", condition.getLoseStopRate() * 100);
            report.append(reportRow).append("\n");
        }

        // 결과 저장
        File reportFile = new File("./backtest-result", "이평선돌파_전략_백테스트_분석결과_" + Timestamp.valueOf(LocalDateTime.now()).getTime() + ".txt");
        FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr");
        System.out.println("결과 파일:" + reportFile.getName());
    }
}
