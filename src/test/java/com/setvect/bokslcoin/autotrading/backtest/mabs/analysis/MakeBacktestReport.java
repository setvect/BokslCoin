package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis;

import com.setvect.bokslcoin.autotrading.backtest.entity.CandleEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.MabsConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.MabsTradeEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepository;
import com.setvect.bokslcoin.autotrading.backtest.repository.MabsConditionEntityRepository;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class MakeBacktestReport {

    @Autowired
    private MabsConditionEntityRepository mabsConditionEntityRepository;
    @Autowired
    private CandleRepository candleRepository;

    @Test
    @Transactional
    public void analysis() throws IOException {
        List<Integer> conditionSeqList = Arrays.asList(
                27288611,// KRW-BTC(2017-10-16)
                27346706,// KRW-ETH(2017-10-10)
                27403421,// KRW-XRP(2017-10-10)
                27458175,// KRW-EOS(2018-03-30)
                27508376,// KRW-ETC(2017-10-09)
                29794493,// KRW-ADA(2017-10-16)
                36879612,// KRW-MANA(2019-04-09)
                36915333,// KRW-BAT(2018-07-30)
                44399001,// KRW-BCH(2017-10-08)
                44544109// KRW-DOT(2020-10-15)
        );

        AnalysisMultiCondition analysisMultiCondition = AnalysisMultiCondition.builder()
                .mabsConditionIdSet(new HashSet<>(conditionSeqList))
//                .mabsConditionIdSet(new HashSet<>(Arrays.asList(32273626)))
                .range(new DateRange(DateUtil.getLocalDateTime("2022-01-10T00:00:00"), LocalDateTime.now()))
//                .range(new DateRange(DateUtil.getLocalDateTime("2021-06-30T00:00:00"), LocalDateTime.now()))
                .investRatio(.99)
                .cash(14_223_714)
                .feeSell(0.002) // 슬립피지까지 고려해 보수적으로 0.2% 수수료 측정
                .feeBuy(0.002)
                .build();
        List<MabsTradeReportItem> tradeReportItems = trading(analysisMultiCondition);
        AnalysisReportResult result = analysis(tradeReportItems, analysisMultiCondition);
        printSummary(result);
        makeReport(result);
        System.out.println("끝");
    }

    @Test
    @Transactional
    public void multiBacktest() throws IOException {
        // 60, 13, 64
        List<Integer> conditionSeqList = Arrays.asList(
                27288611,// KRW-BTC(2017-10-16)
                27346706,// KRW-ETH(2017-10-10)
                27403421,// KRW-XRP(2017-10-10)
                27458175,// KRW-EOS(2018-03-30)
                27508376,// KRW-ETC(2017-10-09)
                29794493,// KRW-ADA(2017-10-16)
                36879612,// KRW-MANA(2019-04-09)
                36915333,// KRW-BAT(2018-07-30)
                44399001,// KRW-BCH(2017-10-08)
                44544109//  KRW-DOT(2020-10-15)
        );
//         60, 1, 64 +- 0.03%
//        List<Integer> conditionSeqList = Arrays.asList(
//                45962518,// KRW-BTC(2017-10-16)
//                46019888,// KRW-ETH(2017-10-10)
//                46077434,// KRW-XRP(2017-10-10)
//                46134607,// KRW-EOS(2018-03-30)
//                46185678,// KRW-ETC(2017-10-09)
//                46242164,// KRW-ADA(2017-10-16)
//                46299027,// KRW-MANA(2019-04-09)
//                46336499,// KRW-BAT(2018-07-30)
//                46382919,// KRW-BCH(2017-10-08)
//                46440134//  KRW-DOT(2020-10-15)
//        );

        //30, 24, 90
//        List<Integer> conditionSeqList = Arrays.asList(
//                44667780,// KRW-BTC(2017-10-16)
//                44783586,// KRW-ETH(2017-10-10)
//                44899267,// KRW-XRP(2017-10-10)
//                45008726,// KRW-EOS(2018-03-30)
//                45109548,// KRW-ETC(2017-10-09)
//                45219094,// KRW-ADA(2017-10-16)
//                45330679,// KRW-MANA(2019-04-09)
//                45400419,// KRW-BAT(2018-07-30)
//                45488562,// KRW-BCH(2017-10-08)
//                45598284//  KRW-DOT(2020-10-15)
//        );
//        List<DateRange> rangeList = Arrays.asList(
//                new DateRange("2020-11-01T00:00:00", "2021-04-14T23:59:59"), // 상승장
//                new DateRange("2021-01-01T00:00:00", "2021-06-08T23:59:59"), // 상승장 후 하락장
//                new DateRange("2020-05-07T00:00:00", "2020-10-20T23:59:59"), // 횡보장1
//                new DateRange("2020-05-08T00:00:00", "2020-07-26T23:59:59"), // 횡보장2
//                new DateRange("2019-06-24T00:00:00", "2020-03-31T23:59:59"), // 횡보+하락장1
//                new DateRange("2017-12-24T00:00:00", "2020-03-31T23:59:59"), // 횡보+하락장2
//                new DateRange("2018-01-01T00:00:00", "2020-11-19T23:59:59"), // 횡보장3
//                new DateRange("2021-04-14T00:00:00", "2021-06-08T23:59:59"), // 하락장1
//                new DateRange("2017-12-07T00:00:00", "2018-02-06T23:59:59"), // 하락장2
//                new DateRange("2018-01-06T00:00:00", "2018-02-06T23:59:59"), // 하락장3
//                new DateRange("2018-01-06T00:00:00", "2018-12-15T23:59:59"), // 하락장4(찐하락장)
//                new DateRange("2019-06-27T00:00:00", "2020-03-17T23:59:59"), // 하락장5
//                new DateRange("2018-01-06T00:00:00", "2019-08-15T23:59:59"), // 하락장 이후 약간의 상승장
//                new DateRange("2021-06-14T00:00:00", "2022-12-31T23:59:59"), // 최근
//                new DateRange("2017-10-01T00:00:00", "2021-06-08T23:59:59"), // 전체 기간1
//                new DateRange("2017-10-01T00:00:00", "2022-12-31T23:59:59")  // 전체 기간2
//        );
        List<DateRange> rangeList = Arrays.asList(
                new DateRange("2017-10-01T00:00:00", "2017-12-31T23:59:59"),
                new DateRange("2018-01-01T00:00:00", "2018-06-30T23:59:59"),
                new DateRange("2018-07-01T00:00:00", "2018-12-31T23:59:59"),
                new DateRange("2019-01-01T00:00:00", "2019-06-30T23:59:59"),
                new DateRange("2019-07-01T00:00:00", "2019-12-31T23:59:59"),
                new DateRange("2020-01-01T00:00:00", "2020-06-30T23:59:59"),
                new DateRange("2020-07-01T00:00:00", "2020-12-31T23:59:59"),
                new DateRange("2021-01-01T00:00:00", "2021-06-30T23:59:59"),
                new DateRange("2021-07-01T00:00:00", "2021-12-31T23:59:59"),
                new DateRange("2018-01-01T00:00:00", "2018-12-31T23:59:59"),
                new DateRange("2019-01-01T00:00:00", "2019-12-31T23:59:59"),
                new DateRange("2020-01-01T00:00:00", "2020-12-31T23:59:59"),
                new DateRange("2021-01-01T00:00:00", "2021-12-31T23:59:59")
        );

        List<AnalysisReportResult> accResult = new ArrayList<>();
        int count = 0;
        int total = rangeList.size() * conditionSeqList.size();
//        for (Integer conditionSeq : conditionSeqList) {
        for (DateRange dateRange : rangeList) {
            AnalysisMultiCondition analysisMultiCondition = AnalysisMultiCondition.builder()
                    .mabsConditionIdSet(new HashSet<>(conditionSeqList))
                    .range(dateRange)
                    .investRatio(.99)
                    .cash(10_000_000)
                    .feeSell(0.0007)
                    .feeBuy(0.0007)
                    .build();
            List<MabsTradeReportItem> tradeReportItems = trading(analysisMultiCondition);
            AnalysisReportResult result = analysis(tradeReportItems, analysisMultiCondition);
            accResult.add(result);
            count++;

            log.info("{}/{}, {} - {}", count, total, dateRange);
        }
//        }

        makeReportMulti(accResult);
        System.out.println("끝");
    }


    /**
     * @param analysisMultiCondition 매매 분석 조건
     * @return 대상코인의 수익률 정보를 제공
     */
    private List<MabsTradeReportItem> trading(AnalysisMultiCondition analysisMultiCondition) throws RuntimeException {
        List<MabsConditionEntity> mabsConditionEntityList = mabsConditionEntityRepository.findAllById(analysisMultiCondition.getMabsConditionIdSet());
        List<List<MabsTradeEntity>> tradeList = mabsConditionEntityList.stream().map(MabsConditionEntity::getMabsTradeEntityList).collect(Collectors.toList());

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

        List<MabsTradeReportItem> reportHistory = new ArrayList<>();

        Set<Integer> ids = analysisMultiCondition.getMabsConditionIdSet();
        int allowBuyCount = ids.size();

        int buyCount = 0;
        double cash = analysisMultiCondition.getCash();

        // 코인명:매수가격
        Map<String, Double> buyCoinAmount = new HashMap<>();

        for (MabsTradeEntity trade : allTrade) {

            MabsTradeReportItem.MabsTradeReportItemBuilder itemBuilder = MabsTradeReportItem.builder();
            itemBuilder.mabsTradeEntity(trade);
            String market = trade.getMabsConditionEntity().getMarket();


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
                double totalBuyAmount = getBuyCoin(buyCoinAmount);
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

                double totalBuyAmount = getBuyCoin(buyCoinAmount);
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
    private AnalysisReportResult analysis(List<MabsTradeReportItem> tradeHistory, AnalysisMultiCondition conditionMulti) {
        List<MabsConditionEntity> conditionByCoin = mabsConditionEntityRepository.findAllById(conditionMulti.getMabsConditionIdSet());
        Set<String> markets = conditionByCoin.stream()
                .map(MabsConditionEntity::getMarket)
                .collect(Collectors.toSet());
        AnalysisReportResult.MultiCoinHoldYield holdYield = calculateCoinHoldYield(conditionMulti.getRange(), markets);

        return AnalysisReportResult.builder()
                .condition(conditionMulti)
                .conditionList(conditionByCoin)
                .tradeHistory(tradeHistory)
                .multiCoinHoldYield(holdYield)
                .totalYield(calculateTotalYield(tradeHistory, conditionMulti))
                .coinWinningRate(calculateCoinInvestment(tradeHistory, conditionByCoin))
                .build();
    }

    /**
     * @param buyCoinAmount 코인 매수 내역
     * @return 코인 매수 총액
     */
    private double getBuyCoin(Map<String, Double> buyCoinAmount) {
        return buyCoinAmount.values().stream().mapToDouble(v -> v).sum();
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
     * 기간동안 보유(존버)할 경우 수익률 계산
     *
     * @param range   투자 기간
     * @param markets 대상 코인
     * @return 기간별 코인 수익률
     */
    private AnalysisReportResult.MultiCoinHoldYield calculateCoinHoldYield(DateRange range, Set<String> markets) {
        Map<String, List<CandleEntity>> coinCandleListMap = markets.stream()
                .collect(Collectors.toMap(Function.identity(),
                        p -> candleRepository.findMarketPrice(p, PeriodType.P_1440, range.getFrom(), range.getTo()))
                );

        Map<String, AnalysisReportResult.YieldMdd> coinByYield = getCoinByYield(coinCandleListMap);
        AnalysisReportResult.YieldMdd sumYield = getYieldMdd(range, coinCandleListMap);

        return AnalysisReportResult.MultiCoinHoldYield.builder()
                .coinByYield(coinByYield)
                .sumYield(sumYield)
                .build();
    }

    /**
     * @param coinCandleListMap 코인명:기간별 캔들 이력
     * @return 코인별 수익률
     * 코인명:수익률
     */
    private Map<String, AnalysisReportResult.YieldMdd> getCoinByYield(Map<String, List<CandleEntity>> coinCandleListMap) {
        return coinCandleListMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
            List<Double> priceList = e.getValue().stream().map(CandleEntity::getOpeningPrice).collect(Collectors.toList());
            AnalysisReportResult.YieldMdd yieldMdd = new AnalysisReportResult.YieldMdd();
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
    private AnalysisReportResult.YieldMdd getYieldMdd(DateRange range, Map<String, List<CandleEntity>> coinCandleListMap) {
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
        AnalysisReportResult.YieldMdd sumYield = new AnalysisReportResult.TotalYield();
        sumYield.setMdd(ApplicationUtil.getMdd(evaluationPrice));
        sumYield.setYield(ApplicationUtil.getYield(evaluationPrice));
        return sumYield;
    }

    /**
     * @param tradeReportItems        거래 내역
     * @param mabsConditionEntityList 코인 매매 조건
     * @return Key: 코인, Value: 수익
     * 코인별 수익 정보
     */
    private Map<String, AnalysisReportResult.WinningRate> calculateCoinInvestment(List<MabsTradeReportItem> tradeReportItems, List<MabsConditionEntity> mabsConditionEntityList) {
        Map<String, AnalysisReportResult.WinningRate> coinInvestmentMap = new TreeMap<>();

        for (MabsConditionEntity mabsConditionEntity : mabsConditionEntityList) {
            AnalysisReportResult.WinningRate coinInvestment = calculateInvestment(mabsConditionEntity.getMarket(), tradeReportItems);
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
    private AnalysisReportResult.TotalYield calculateTotalYield(List<MabsTradeReportItem> tradeReportItems, AnalysisMultiCondition analysisMultiCondition) {

        AnalysisReportResult.TotalYield totalYield = new AnalysisReportResult.TotalYield();
        long dayCount = analysisMultiCondition.getRange().getDiffDays();
        totalYield.setDayCount((int) dayCount);

        if (tradeReportItems.isEmpty()) {
            totalYield.setYield(0);
            totalYield.setMdd(0);
            return totalYield;
        }

        double realYield = tradeReportItems.get(tradeReportItems.size() - 1).getFinalResult() / tradeReportItems.get(0).getFinalResult() - 1;
        List<Double> finalResultList = tradeReportItems.stream().map(MabsTradeReportItem::getFinalResult).collect(Collectors.toList());
        double realMdd = ApplicationUtil.getMdd(finalResultList);
        totalYield.setYield(realYield);
        totalYield.setMdd(realMdd);

        // 승률 계산
        for (MabsTradeReportItem mabsTradeReportItem : tradeReportItems) {
            if (mabsTradeReportItem.getMabsTradeEntity().getTradeType() == TradeType.BUY) {
                continue;
            }
            if (mabsTradeReportItem.getGains() > 0) {
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
     * @param market       코인
     * @param tradeHistory 매매 기록
     * @return 코인별 투자전략 수익률
     */
    private static AnalysisReportResult.WinningRate calculateInvestment(String market, List<MabsTradeReportItem> tradeHistory) {
        List<MabsTradeReportItem> filter = tradeHistory.stream()
                .filter(p -> p.getMabsTradeEntity().getMabsConditionEntity().getMarket().equals(market))
                .filter(p -> p.getMabsTradeEntity().getTradeType() == TradeType.SELL)
                .collect(Collectors.toList());
        double totalInvest = filter.stream().mapToDouble(MabsTradeReportItem::getGains).sum();
        int gainCount = (int) filter.stream().filter(p -> p.getGains() > 0).count();
        AnalysisReportResult.WinningRate coinInvestment = new AnalysisReportResult.WinningRate();
        coinInvestment.setInvest(totalInvest);
        coinInvestment.setGainCount(gainCount);
        coinInvestment.setLossCount(filter.size() - gainCount);
        return coinInvestment;
    }


    /**
     * 분석 요약결과
     *
     * @param result ..
     */
    private void printSummary(AnalysisReportResult result) {
        StringBuilder report = new StringBuilder();
        AnalysisReportResult.MultiCoinHoldYield multiCoinHoldYield = result.getMultiCoinHoldYield();
        AnalysisReportResult.YieldMdd sumYield = multiCoinHoldYield.getSumYield();
        report.append(String.format("동일비중 수익\t %,.2f%%", sumYield.getYield() * 100)).append("\n");
        report.append(String.format("동일비중 MDD\t %,.2f%%", sumYield.getMdd() * 100)).append("\n");
        report.append("\n-----------\n");

        for (Map.Entry<String, AnalysisReportResult.WinningRate> entry : result.getCoinWinningRate().entrySet()) {
            String market = entry.getKey();
            AnalysisReportResult.WinningRate coinInvestment = entry.getValue();
            report.append(String.format("[%s] 수익금액 합계\t %,.0f", market, coinInvestment.getInvest())).append("\n");
            report.append(String.format("[%s] 매매 횟수\t %d", market, coinInvestment.getTradeCount())).append("\n");
            report.append(String.format("[%s] 승률\t %,.2f%%", market, coinInvestment.getWinRate() * 100)).append("\n");
        }

        report.append("\n-----------\n");
        AnalysisReportResult.TotalYield totalYield = result.getTotalYield();
        report.append(String.format("실현 수익\t %,.2f%%", totalYield.getYield() * 100)).append("\n");
        report.append(String.format("실현 MDD\t %,.2f%%", totalYield.getMdd() * 100)).append("\n");
        report.append(String.format("매매회수\t %d", totalYield.getTradeCount())).append("\n");
        report.append(String.format("승률\t %,.2f%%", totalYield.getWinRate() * 100)).append("\n");
        report.append(String.format("CAGR\t %,.2f%%", totalYield.getCagr() * 100)).append("\n");

        System.out.println(report);
    }

    /**
     * 분석결과 리포트 만듦
     *
     * @param result 분석결과
     * @throws IOException .
     */
    private void makeReport(AnalysisReportResult result) throws IOException {
        String header = "날짜(KST),날짜(UTC),코인,매매구분,단기 이동평균, 장기 이동평균,매수 체결 가격,최고수익률,최저수익률,매도 체결 가격,매도 이유,실현 수익률,매수금액,전체코인 매수금액,현금,수수료,투자 수익(수수료포함),투자 결과,현금 + 전체코인 매수금액 - 수수료,수익비";
        StringBuilder report = new StringBuilder(header.replace(",", "\t")).append("\n");
        for (MabsTradeReportItem row : result.getTradeHistory()) {
            MabsTradeEntity mabsTradeEntity = row.getMabsTradeEntity();
            MabsConditionEntity mabsConditionEntity = mabsTradeEntity.getMabsConditionEntity();
            LocalDateTime tradeTimeKst = mabsTradeEntity.getTradeTimeKst();
            String dateKst = DateUtil.formatDateTime(tradeTimeKst);
            LocalDateTime utcTime = convertUtc(tradeTimeKst);
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
        AnalysisReportResult.MultiCoinHoldYield multiCoinHoldYield = result.getMultiCoinHoldYield();
        for (Map.Entry<String, AnalysisReportResult.YieldMdd> coinHoldYield : multiCoinHoldYield.getCoinByYield().entrySet()) {
            String market = coinHoldYield.getKey();
            AnalysisReportResult.YieldMdd coinYield = coinHoldYield.getValue();
            report.append(String.format("[%s] 실제 수익\t %,.2f%%", market, coinYield.getYield() * 100)).append("\n");
            report.append(String.format("[%s] 실제 MDD\t %,.2f%%", market, coinYield.getMdd() * 100)).append("\n");
        }
        AnalysisReportResult.YieldMdd sumYield = multiCoinHoldYield.getSumYield();
        report.append(String.format("동일비중 수익\t %,.2f%%", sumYield.getYield() * 100)).append("\n");
        report.append(String.format("동일비중 MDD\t %,.2f%%", sumYield.getMdd() * 100)).append("\n");

        report.append("\n-----------\n");


        for (Map.Entry<String, AnalysisReportResult.WinningRate> entry : result.getCoinWinningRate().entrySet()) {
            String market = entry.getKey();
            AnalysisReportResult.WinningRate coinInvestment = entry.getValue();
            report.append(String.format("[%s] 수익금액 합계\t %,.0f", market, coinInvestment.getInvest())).append("\n");
            report.append(String.format("[%s] 매매 횟수\t %d", market, coinInvestment.getTradeCount())).append("\n");
            report.append(String.format("[%s] 승률\t %,.2f%%", market, coinInvestment.getWinRate() * 100)).append("\n");

        }
        report.append("\n-----------\n");
        AnalysisReportResult.TotalYield totalYield = result.getTotalYield();
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
            report.append(String.format("조건아이디\t %s", mabsConditionEntity.getMabsConditionSeq())).append("\n");
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
     * @throws IOException ..
     */
    private void makeReportMulti(List<AnalysisReportResult> accResult) throws IOException {
        String header = "분석기간,분석 아이디,대상 코인,투자비율,최초 투자금액,매수 수수료,매도 수수료,조건 설명," +
                "매수 후 보유 수익,매수 후 보유 MDD,실현 수익,실현 MDD,매매 횟수,승률,CAGR";
        StringBuilder report = new StringBuilder(header.replace(",", "\t") + "\n");

        // 1. 각 매매 결과
        for (AnalysisReportResult result : accResult) {
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
            AnalysisReportResult.MultiCoinHoldYield multiCoinHoldYield = result.getMultiCoinHoldYield();
            AnalysisReportResult.YieldMdd sumYield = multiCoinHoldYield.getSumYield();

            reportRow.append(String.format("%,.2f%%\t", sumYield.getYield() * 100));
            reportRow.append(String.format("%,.2f%%\t", sumYield.getMdd() * 100));

            AnalysisReportResult.TotalYield totalYield = result.getTotalYield();
            reportRow.append(String.format("%,.2f%%\t", totalYield.getYield() * 100));
            reportRow.append(String.format("%,.2f%%\t", totalYield.getMdd() * 100));
            reportRow.append(String.format("%d\t", totalYield.getTradeCount()));
            reportRow.append(String.format("%,.2f%%\t", totalYield.getWinRate() * 100));
            reportRow.append(String.format("%,.2f%%\t", totalYield.getCagr() * 100));

            report.append(reportRow).append("\n");
        }

        // 2. 사용한 매매 조건 정보
        report.append("\n----------------\n");
        report.append("조건들\n");
        String condHeader = "조건 아이디,분석주기,대상 코인,상승 매수률,하락 매도률,단기 이동평균,장기 이동평균,손절률";
        report.append(condHeader.replace(",", "\t")).append("\n");
        List<MabsConditionEntity> conditionAll = accResult.stream()
                .flatMap(p -> p.getConditionList().stream())
                .distinct()
                .sorted(Comparator.comparingInt(MabsConditionEntity::getMabsConditionSeq))
                .collect(Collectors.toList());

        for (MabsConditionEntity condition : conditionAll) {
            String reportRow = String.format("%s\t", condition.getMabsConditionSeq()) +
                    String.format("%s\t", condition.getTradePeriod()) +
                    String.format("%s\t", condition.getMarket()) +
                    String.format("%,.2f%%\t", condition.getUpBuyRate() * 100) +
                    String.format("%,.2f%%\t", condition.getDownSellRate() * 100) +
                    String.format("%d\t", condition.getShortPeriod()) +
                    String.format("%d\t", condition.getLongPeriod()) +
                    String.format("%,.2f%%\t", condition.getLoseStopRate() * 100);
            report.append(reportRow).append("\n");
        }

        // 3. 결과 저장
        File reportFile = new File("./backtest-result", "이평선돌파_전략_백테스트_분석결과_" + Timestamp.valueOf(LocalDateTime.now()).getTime() + ".txt");
        FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr");
        System.out.println("결과 파일:" + reportFile.getName());

    }

    private static LocalDateTime convertUtc(LocalDateTime datetime) {
        return datetime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
