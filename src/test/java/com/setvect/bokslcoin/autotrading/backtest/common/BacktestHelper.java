package com.setvect.bokslcoin.autotrading.backtest.common;

import com.setvect.bokslcoin.autotrading.backtest.common.model.AnalysisMultiCondition;
import com.setvect.bokslcoin.autotrading.backtest.common.model.CommonAnalysisReportResult;
import com.setvect.bokslcoin.autotrading.backtest.common.model.CommonTradeReportItem;
import com.setvect.bokslcoin.autotrading.backtest.entity.CandleEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.common.CommonConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.common.CommonTradeEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsConditionEntity;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
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

    /**
     * @param accResult ..
     * @return 각 매매 결과
     */
    @NotNull
    public static String makeReportMultiList(List<? extends CommonAnalysisReportResult<? extends CommonConditionEntity, ? extends CommonTradeEntity>> accResult) {
        String header = "분석기간,분석 아이디,대상 코인,투자비율,최초 투자금액,매수 수수료,매도 수수료,조건 설명," +
                "매수 후 보유 수익,매수 후 보유 MDD,실현 수익,실현 MDD,매매 횟수,승률,CAGR";
        StringBuilder report = new StringBuilder(header.replace(",", "\t") + "\n");

        for (CommonAnalysisReportResult<? extends CommonConditionEntity, ? extends CommonTradeEntity> result : accResult) {
            AnalysisMultiCondition multiCondition = result.getCondition();

            StringBuilder reportRow = new StringBuilder();
            reportRow.append(String.format("%s\t", multiCondition.getRange()));
            reportRow.append(String.format("%s\t", StringUtils.join(result.getMabsConditionIds(), ", ")));
            reportRow.append(String.format("%s\t", StringUtils.join(result.getMarkets(), ", ")));
            reportRow.append(String.format("%,.2f%%\t", multiCondition.getInvestRatio() * 100));
            reportRow.append(String.format("%,.0f\t", multiCondition.getCash()));
            reportRow.append(String.format("%,.2f%%\t", multiCondition.getFeeBuy() * 100));
            reportRow.append(String.format("%,.2f%%\t", multiCondition.getFeeSell() * 100));
            reportRow.append(String.format("%s\t", multiCondition.getComment()));
            CommonAnalysisReportResult.MultiCoinHoldYield multiCoinHoldYield = result.getMultiCoinHoldYield();
            CommonAnalysisReportResult.YieldMdd sumYield = multiCoinHoldYield.getSumYield();

            reportRow.append(String.format("%,.2f%%\t", sumYield.getYield() * 100));
            reportRow.append(String.format("%,.2f%%\t", sumYield.getMdd() * 100));

            CommonAnalysisReportResult.TotalYield totalYield = result.getTotalYield();
            reportRow.append(String.format("%,.2f%%\t", totalYield.getYield() * 100));
            reportRow.append(String.format("%,.2f%%\t", totalYield.getMdd() * 100));
            reportRow.append(String.format("%d\t", totalYield.getTradeCount()));
            reportRow.append(String.format("%,.2f%%\t", totalYield.getWinRate() * 100));
            reportRow.append(String.format("%,.2f%%\t", totalYield.getCagr() * 100));

            report.append(reportRow).append("\n");
        }
        return report.toString();
    }

    /**
     * 분석 요약결과
     *
     * @param result ..
     */
    public static void printSummary(CommonAnalysisReportResult<? extends CommonConditionEntity, ? extends CommonTradeEntity> result) {
        StringBuilder report = new StringBuilder();
        CommonAnalysisReportResult.MultiCoinHoldYield multiCoinHoldYield = result.getMultiCoinHoldYield();
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

        System.out.println(report);
    }

    /**
     * @param tradeEntityList 매매 내역
     * @param range           대상 범위
     * @return 매매 내역에서 range 범위에 해당하는 것만 반환
     */
    public static <T extends CommonTradeEntity> List<T> subTrade(List<T> tradeEntityList, DateRange range) {
        return tradeEntityList
                .stream()
                .filter(pp -> range.isBetween(pp.getTradeTimeKst()))
                .collect(Collectors.toList());
    }

    /**
     * 첫 거래는 매수로 시작하고 마지막 거래는 매도로 끝나야됨
     *
     * @param targetTradeHistory 거래 내역
     * @return 첫 거래는 매수, 마지막 거래는 매도로 끝나는 거래 내역
     */
    public static <T extends CommonTradeEntity> List<T> makePairTrade(List<T> targetTradeHistory) {
        int skip = targetTradeHistory.get(0).getTradeType() == TradeType.BUY ? 0 : 1;
        int size = targetTradeHistory.size();
        int limit = targetTradeHistory.get(size - 1).getTradeType() == TradeType.SELL ? size - skip : size - skip - 1;
        return targetTradeHistory.stream()
                .skip(skip)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * @param analysisMultiCondition 매매 조건
     * @param conditionEntityList    매매 조건
     * @param <T>                    건별 매매 내역 타입
     * @return 통합 매매 이력
     */
    public static <T extends CommonTradeEntity> List<CommonTradeReportItem<T>> trading(
            AnalysisMultiCondition analysisMultiCondition,
            List<? extends CommonConditionEntity> conditionEntityList
    ) {
        DateRange range = analysisMultiCondition.getRange();

        List<T> allTrade = conditionEntityList.stream()
                .flatMap(
                        p -> {
                            List<T> targetTradeHistory = BacktestHelper.subTrade(p.getTradeEntityList(), range);
                            List<T> list = BacktestHelper.makePairTrade(targetTradeHistory);
                            return list.stream();
                        }
                )
                .sorted(Comparator.comparing(CommonTradeEntity::getTradeTimeKst))
                .collect(Collectors.toList());


        List<CommonTradeReportItem<T>> reportHistory = new ArrayList<>();

        Set<Integer> ids = analysisMultiCondition.getConditionIdSet();
        int allowBuyCount = ids.size();

        int buyCount = 0;
        double cash = analysisMultiCondition.getCash();

        // 코인명:매수가격
        Map<String, Double> buyCoinAmount = new HashMap<>();

        for (T trade : allTrade) {
            CommonTradeReportItem.CommonTradeReportItemBuilder<T> itemBuilder = CommonTradeReportItem.builder();
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
            CommonTradeReportItem<T> build = itemBuilder.build();
            reportHistory.add(build);
        }
        return reportHistory;
    }
}
