package com.setvect.bokslcoin.autotrading.backtest.common;

import com.setvect.bokslcoin.autotrading.backtest.common.model.AnalysisMultiCondition;
import com.setvect.bokslcoin.autotrading.backtest.common.model.CommonAnalysisReportResult;
import com.setvect.bokslcoin.autotrading.backtest.common.model.CommonTradeReportItem;
import com.setvect.bokslcoin.autotrading.backtest.entity.CandleEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.common.CommonTradeEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsConditionEntity;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BacktestHelper {
    private BacktestHelper() {
        // not instance
    }

    public static Candle depthCopy(Candle candle) {
        String json = GsonUtil.GSON.toJson(candle);
        return GsonUtil.GSON.fromJson(json, Candle.class);
    }

    /**
     * @param accountMap 코인(현금 포함) 계좌
     * @return 현재 투자한 코인 함
     */
    public static double getBuyTotalAmount(Map<String, Account> accountMap) {
        return accountMap.entrySet().stream().filter(e -> !e.getKey().equals("KRW")).mapToDouble(e -> e.getValue().getInvestCash()).sum();
    }

    /**
     * @param buyCoinAmount 코인 매수 내역
     * @return 코인 매수 총액
     */
    public static double getBuyCoin(Map<String, Double> buyCoinAmount) {
        return buyCoinAmount.values().stream().mapToDouble(v -> v).sum();
    }

    public static LocalDateTime convertUtc(LocalDateTime datetime) {
        return datetime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /**
     * @param tradeReportItems        거래 내역
     * @param mabsConditionEntityList 코인 매매 조건
     * @return Key: 코인, Value: 수익
     * 코인별 수익 정보
     */
    public static Map<String, CommonAnalysisReportResult.WinningRate> calculateCoinInvestment(
            List<? extends CommonTradeReportItem<? extends CommonTradeEntity>> tradeReportItems,
            List<MabsConditionEntity> mabsConditionEntityList) {
        Map<String, CommonAnalysisReportResult.WinningRate> coinInvestmentMap = new TreeMap<>();

        for (MabsConditionEntity mabsConditionEntity : mabsConditionEntityList) {
            CommonAnalysisReportResult.WinningRate coinInvestment = BacktestHelper.calculateInvestment(mabsConditionEntity.getMarket(), tradeReportItems);
            coinInvestmentMap.put(mabsConditionEntity.getMarket(), coinInvestment);
        }
        return coinInvestmentMap;
    }

    /**
     * 멀티 코인 매매 수익정보 계산
     *
     * @param tradeReportItems       거래 내역
     * @param analysisMultiCondition 분석 조건
     * @return 수익률 정보
     */
    public static CommonAnalysisReportResult.TotalYield calculateTotalYield(
            List<? extends CommonTradeReportItem<? extends CommonTradeEntity>> tradeReportItems,
            AnalysisMultiCondition analysisMultiCondition) {
        CommonAnalysisReportResult.TotalYield totalYield = new CommonAnalysisReportResult.TotalYield();
        long dayCount = analysisMultiCondition.getRange().getDiffDays();
        totalYield.setDayCount((int) dayCount);

        if (tradeReportItems.isEmpty()) {
            totalYield.setYield(0);
            totalYield.setMdd(0);
            return totalYield;
        }

        double realYield = tradeReportItems.get(tradeReportItems.size() - 1).getFinalResult() / tradeReportItems.get(0).getFinalResult() - 1;
        List<Double> finalResultList = tradeReportItems.stream().map(CommonTradeReportItem::getFinalResult).collect(Collectors.toList());
        double realMdd = ApplicationUtil.getMdd(finalResultList);
        totalYield.setYield(realYield);
        totalYield.setMdd(realMdd);

        // 승률 계산
        for (CommonTradeReportItem<? extends CommonTradeEntity> commonTradeReportItem : tradeReportItems) {
            if (commonTradeReportItem.getTradeEntity().getTradeType() == TradeType.BUY) {
                continue;
            }
            if (commonTradeReportItem.getGains() > 0) {
                totalYield.incrementGainCount();
            } else {
                totalYield.incrementLossCount();
            }
        }
        return totalYield;
    }

    /**
     * 개별 코인 수익 정보 계산
     *
     * @param market           코인
     * @param tradeReportItems 매매 기록
     * @return 코인별 투자전략 수익률
     */
    public static CommonAnalysisReportResult.WinningRate calculateInvestment(
            String market,
            List<? extends CommonTradeReportItem<? extends CommonTradeEntity>> tradeReportItems
    ) {
        List<? extends CommonTradeReportItem<? extends CommonTradeEntity>> filter = tradeReportItems.stream()
                .filter(p -> p.getTradeEntity().getConditionEntity().getMarket().equals(market))
                .filter(p -> p.getTradeEntity().getTradeType() == TradeType.SELL)
                .collect(Collectors.toList());
        double totalInvest = filter.stream().mapToDouble(CommonTradeReportItem::getGains).sum();
        int gainCount = (int) filter.stream().filter(p -> p.getGains() > 0).count();
        CommonAnalysisReportResult.WinningRate coinInvestment = new CommonAnalysisReportResult.WinningRate();
        coinInvestment.setInvest(totalInvest);
        coinInvestment.setGainCount(gainCount);
        coinInvestment.setLossCount(filter.size() - gainCount);
        return coinInvestment;
    }

    /**
     * @param coinCandleListMap 코인명:기간별 캔들 이력
     * @return 코인별 수익률
     * 코인명:수익률
     */
    static Map<String, CommonAnalysisReportResult.YieldMdd> getCoinByYield(Map<String, List<CandleEntity>> coinCandleListMap) {
        return coinCandleListMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
            List<Double> priceList = e.getValue().stream().map(CandleEntity::getOpeningPrice).collect(Collectors.toList());
            CommonAnalysisReportResult.YieldMdd yieldMdd = new CommonAnalysisReportResult.YieldMdd();
            yieldMdd.setYield(ApplicationUtil.getYield(priceList));
            yieldMdd.setMdd(ApplicationUtil.getMdd(priceList));
            return yieldMdd;
        }));
    }

    /**
     * @param range             투자 기간
     * @param coinCandleListMap 코인명:기간별 캔들 이력
     * @return 해당 기간 동안 동일 비중 투자 시 수익률
     */
    static CommonAnalysisReportResult.YieldMdd getYieldMdd(DateRange range, Map<String, List<CandleEntity>> coinCandleListMap) {
        LocalDateTime start = range.getFrom();
        // 코인 시작 가격 <코인명:가격>
        Map<String, Double> coinStartPrice = coinCandleListMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, p -> {
                    if (p.getValue().isEmpty()) {
                        return 0.0;
                    }
                    return p.getValue().get(0).getOpeningPrice();
                }));

        // <코인명:수익률>
        Map<String, Double> coinYield = coinCandleListMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, p -> 1.0));

        // 날짜별 수익률 합계
        List<Double> evaluationPrice = new ArrayList<>();
        evaluationPrice.add((coinYield.values().stream().mapToDouble(p -> p).sum()));

        // <코인명:<날짜:캔들정보>>
        Map<String, Map<LocalDate, CandleEntity>> coinCandleMapByDate = coinCandleListMap
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        p -> p.getValue().stream().collect(Collectors.toMap(c -> c.getCandleDateTimeKst().toLocalDate(), Function.identity()))));

        while (start.isBefore(range.getTo()) || start.isEqual(range.getTo())) {
            LocalDate date = start.toLocalDate();
            for (Map.Entry<String, Double> entity : coinStartPrice.entrySet()) {
                String market = entity.getKey();
                Double startPrice = entity.getValue();
                CandleEntity candle = coinCandleMapByDate.get(market).get(date);
                if (candle == null) {
                    continue;
                }

                double yield = candle.getTradePrice() / startPrice;
                coinYield.put(market, yield);
            }
            evaluationPrice.add((coinYield.values().stream().mapToDouble(p -> p).sum()));
            start = start.plusDays(1);
        }
        CommonAnalysisReportResult.YieldMdd sumYield = new CommonAnalysisReportResult.TotalYield();
        sumYield.setMdd(ApplicationUtil.getMdd(evaluationPrice));
        sumYield.setYield(ApplicationUtil.getYield(evaluationPrice));
        return sumYield;
    }

}
