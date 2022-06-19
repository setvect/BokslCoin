package com.setvect.bokslcoin.autotrading.backtest.neovbs;

import com.setvect.bokslcoin.autotrading.algorithm.BasicTradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.common.TradeCommonService;
import com.setvect.bokslcoin.autotrading.algorithm.neovbs.NeoVbsMultiProperties;
import com.setvect.bokslcoin.autotrading.algorithm.neovbs.NeoVbsMultiService;
import com.setvect.bokslcoin.autotrading.algorithm.websocket.TradeResult;
import com.setvect.bokslcoin.autotrading.backtest.common.AnalysisMultiCondition;
import com.setvect.bokslcoin.autotrading.backtest.common.CandleDataProvider;
import com.setvect.bokslcoin.autotrading.backtest.entity.CandleEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.backtest.entity.neovbs.NeoVbsConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.neovbs.NeoVbsTradeEntity;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepositoryCustom;
import com.setvect.bokslcoin.autotrading.backtest.repository.NeoVbsConditionEntityRepository;
import com.setvect.bokslcoin.autotrading.backtest.repository.NeoVbsTradeEntityRepository;
import com.setvect.bokslcoin.autotrading.exchange.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.OrderService;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import com.setvect.bokslcoin.autotrading.record.repository.AssetHistoryRepository;
import com.setvect.bokslcoin.autotrading.record.repository.TradeRepository;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import javax.transaction.Transactional;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class NeoVbsBacktest {
    /**
     * 투자금
     */
    public static final double CASH = 10_000_000;

    @Autowired
    private NeoVbsConditionEntityRepository neoVbsConditionEntityRepository;

    @Autowired
    private NeoVbsTradeEntityRepository neoVbsTradeEntityRepository;

    @Autowired
    private CandleRepositoryCustom candleRepositoryCustom;

    @Autowired
    private AssetHistoryRepository assetHistoryRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Mock
    private SlackMessageService slackMessageService;
    @Spy
    private final TradeEvent tradeEvent = new BasicTradeEvent(slackMessageService);
    @Mock
    private AccountService accountService;
    @Mock
    private CandleService candleService;
    @Mock
    private OrderService orderService;
    @InjectMocks
    private TradeCommonService tradeCommonService;

    @InjectMocks
    private NeoVbsMultiService neoVbsMultiService;

    private List<NeoVbsMultiBacktestRow> tradeHistory;

    /**
     * 최고 수익률
     */
    private double highYield;
    /**
     * 최저 수익률
     */
    private double lowYield;
    private Map<String, CurrentPrice> priceMap;
    /**
     * 종목: 매수 목표가
     */
    private Map<String, Double> targetPriceMap;
    private Map<String, Account> accountMap;

    private static Candle depthCopy(Candle candle) {
        String json = GsonUtil.GSON.toJson(candle);
        return GsonUtil.GSON.fromJson(json, Candle.class);
    }

    private static LocalDateTime convertUtc(LocalDateTime datetime) {
        return datetime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /**
     * 개별 코인 수익 정보 계산
     *
     * @param market       코인
     * @param tradeHistory 매매 기록
     * @return 코인별 투자전략 수익률
     */
    private static AnalysisReportResult.WinningRate calculateInvestment(String market, List<NeoVbsTradeReportItem> tradeHistory) {
        List<NeoVbsTradeReportItem> filter = tradeHistory.stream()
                .filter(p -> p.getNeoVbsTradeEntity().getVbsConditionEntity().getMarket().equals(market))
                .filter(p -> p.getNeoVbsTradeEntity().getTradeType() == TradeType.SELL)
                .collect(Collectors.toList());
        double totalInvest = filter.stream().mapToDouble(NeoVbsTradeReportItem::getGains).sum();
        int gainCount = (int) filter.stream().filter(p -> p.getGains() > 0).count();
        AnalysisReportResult.WinningRate coinInvestment = new AnalysisReportResult.WinningRate();
        coinInvestment.setInvest(totalInvest);
        coinInvestment.setGainCount(gainCount);
        coinInvestment.setLossCount(filter.size() - gainCount);
        return coinInvestment;
    }

    /**
     * DB 저장안함
     */
    @Test
    @Transactional
    public void 변동성돌파전략_1회성_백테스트() {
        List<NeoVbsConditionEntity> neoVbsConditionEntities = backtest();
        List<Integer> conditionSeqList = neoVbsConditionEntities.stream().map(p -> p.getVbsConditionSeq()).collect(Collectors.toList());
        AnalysisMultiCondition analysisMultiCondition = AnalysisMultiCondition.builder()
                .conditionIdSet(new HashSet<>(conditionSeqList))
                .range(new DateRange(DateUtil.getLocalDateTime("2022-05-30T00:00:00"), LocalDateTime.now()))
                .investRatio(.99)
                .cash(10_000_000)
                .feeSell(0.0007)
                .feeBuy(0.0007)
                .build();
        List<NeoVbsTradeReportItem> tradeReportItems = trading(analysisMultiCondition);
        AnalysisReportResult result = analysis(tradeReportItems, analysisMultiCondition);
        printSummary(result);
        makeReport(result);
        System.out.println("끝");
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
     */
    @SneakyThrows
    private void makeReport(AnalysisReportResult result) {
        String header = "날짜(KST),날짜(UTC),코인,매매구분,목표가,매수 체결 가격,최고수익률,최저수익률,매도 체결 가격,매도 이유,실현 수익률,매수금액,전체코인 매수금액,현금,수수료,투자 수익(수수료포함),투자 결과,현금 + 전체코인 매수금액 - 수수료,수익비";
        StringBuilder report = new StringBuilder(header.replace(",", "\t")).append("\n");
        for (NeoVbsTradeReportItem row : result.getTradeHistory()) {
            NeoVbsTradeEntity neoVbsTradeEntity = row.getNeoVbsTradeEntity();
            NeoVbsConditionEntity neoVbsConditionEntity = neoVbsTradeEntity.getVbsConditionEntity();
            LocalDateTime tradeTimeKst = neoVbsTradeEntity.getTradeTimeKst();
            String dateKst = DateUtil.formatDateTime(tradeTimeKst);
            LocalDateTime utcTime = convertUtc(tradeTimeKst);
            String dateUtc = DateUtil.formatDateTime(utcTime);

            report.append(String.format("%s\t", dateKst));
            report.append(String.format("%s\t", dateUtc));
            report.append(String.format("%s\t", neoVbsConditionEntity.getMarket()));
            report.append(String.format("%s\t", neoVbsTradeEntity.getTradeType()));
            report.append(String.format("%,.0f\t", neoVbsTradeEntity.getTargetPrice()));
            report.append(String.format("%,.0f\t", row.getBuyAmount()));
            report.append(String.format("%,.2f%%\t", neoVbsTradeEntity.getHighYield() * 100));
            report.append(String.format("%,.2f%%\t", neoVbsTradeEntity.getLowYield() * 100));
            report.append(String.format("%,.0f\t", neoVbsTradeEntity.getUnitPrice()));
            report.append(String.format("%s\t", neoVbsTradeEntity.getSellReason() == null ? "" : neoVbsTradeEntity.getSellReason()));
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

        for (NeoVbsConditionEntity mabsConditionEntity : result.getConditionList()) {
            report.append("\n---\n");
            report.append(String.format("조건아이디\t %s", mabsConditionEntity.getVbsConditionSeq())).append("\n");
            report.append(String.format("분석주기\t %s", mabsConditionEntity.getTradePeriod())).append("\n");
            report.append(String.format("대상 코인\t %s", mabsConditionEntity.getMarket())).append("\n");
            report.append(String.format("K\t %,.2f%%", mabsConditionEntity.getK())).append("\n");
            report.append(String.format("손절\t %,.2f%%", mabsConditionEntity.getLoseStopRate() * 100)).append("\n");
        }

        String coins = StringUtils.join(result.getMarkets(), ", ");
        String reportFileName = String.format("[%s~%s]_[%s].txt", range.getFromDateFormat(), range.getToDateFormat(), coins);

        File reportFile = new File("./backtest-result", reportFileName);
        FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr");
        System.out.println("결과 파일:" + reportFile.getName());
    }

    /**
     * @param tradeHistory   매매 내역
     * @param conditionMulti 조건
     * @return 분석결과
     */
    private AnalysisReportResult analysis(List<NeoVbsTradeReportItem> tradeHistory, AnalysisMultiCondition conditionMulti) {
        List<NeoVbsConditionEntity> conditionByCoin = neoVbsConditionEntityRepository.findAllById(conditionMulti.getConditionIdSet());
        Set<String> markets = conditionByCoin.stream()
                .map(NeoVbsConditionEntity::getMarket)
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
     * @param tradeReportItems          거래 내역
     * @param neoVbsConditionEntityList 코인 매매 조건
     * @return Key: 코인, Value: 수익
     * 코인별 수익 정보
     */
    private Map<String, AnalysisReportResult.WinningRate> calculateCoinInvestment(List<NeoVbsTradeReportItem> tradeReportItems, List<NeoVbsConditionEntity> neoVbsConditionEntityList) {
        Map<String, AnalysisReportResult.WinningRate> coinInvestmentMap = new TreeMap<>();

        for (NeoVbsConditionEntity neoVbsConditionEntity : neoVbsConditionEntityList) {
            AnalysisReportResult.WinningRate coinInvestment = calculateInvestment(neoVbsConditionEntity.getMarket(), tradeReportItems);
            coinInvestmentMap.put(neoVbsConditionEntity.getMarket(), coinInvestment);
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
    private AnalysisReportResult.TotalYield calculateTotalYield(List<NeoVbsTradeReportItem> tradeReportItems, AnalysisMultiCondition analysisMultiCondition) {

        AnalysisReportResult.TotalYield totalYield = new AnalysisReportResult.TotalYield();
        long dayCount = analysisMultiCondition.getRange().getDiffDays();
        totalYield.setDayCount((int) dayCount);

        if (tradeReportItems.isEmpty()) {
            totalYield.setYield(0);
            totalYield.setMdd(0);
            return totalYield;
        }

        double realYield = tradeReportItems.get(tradeReportItems.size() - 1).getFinalResult() / tradeReportItems.get(0).getFinalResult() - 1;
        List<Double> finalResultList = tradeReportItems.stream().map(NeoVbsTradeReportItem::getFinalResult).collect(Collectors.toList());
        double realMdd = ApplicationUtil.getMdd(finalResultList);
        totalYield.setYield(realYield);
        totalYield.setMdd(realMdd);

        // 승률 계산
        for (NeoVbsTradeReportItem mabsTradeReportItem : tradeReportItems) {
            if (mabsTradeReportItem.getNeoVbsTradeEntity().getTradeType() == TradeType.BUY) {
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
     * 기간동안 보유(존버)할 경우 수익률 계산
     *
     * @param range   투자 기간
     * @param markets 대상 코인
     * @return 기간별 코인 수익률
     */
    // TODO 공통 모듈
    private AnalysisReportResult.MultiCoinHoldYield calculateCoinHoldYield(DateRange range, Set<String> markets) {
        Map<String, List<CandleEntity>> coinCandleListMap = markets.stream()
                .collect(Collectors.toMap(Function.identity(),
                        p -> candleRepositoryCustom.findMarketPrice(p, PeriodType.PERIOD_1440, range.getFrom(), range.getTo()))
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
    // TODO 공통 모듈
    private Map<String, AnalysisReportResult.YieldMdd> getCoinByYield(Map<String, List<CandleEntity>> coinCandleListMap) {
        return coinCandleListMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
            List<Double> priceList = e.getValue().stream().map(CandleEntity::getOpeningPrice).collect(Collectors.toList());
            AnalysisReportResult.YieldMdd yieldMdd = new AnalysisReportResult.YieldMdd();
            yieldMdd.setYield(ApplicationUtil.getYield(priceList));
            yieldMdd.setMdd(ApplicationUtil.getMdd(priceList));
            return yieldMdd;
        }));
    }
    // TODO 공통 모듈

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
     * @return 백테스트 조건
     */
    private List<NeoVbsConditionEntity> backtest() {
        //        List<String> markets = Arrays.asList("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-EOS", "KRW-ETC", "KRW-ADA", "KRW-MANA", "KRW-BAT", "KRW-BCH", "KRW-DOT");
        List<String> markets = Arrays.asList("KRW-XRP");
        List<NeoVbsTradeEntity> neoVbsTradeEntities = Collections.emptyList();

        List<NeoVbsConditionEntity> neoVbsConditionEntities = new ArrayList<>();
        for (String market : markets) {
            DateRange range = new DateRange(DateUtil.getLocalDateTime("2022-05-30T00:00:00"), LocalDateTime.now());
            NeoVbsConditionEntity condition = NeoVbsConditionEntity.builder()
                    .market(market)
                    .tradePeriod(PeriodType.PERIOD_1440)
                    .k(0.5)
                    .loseStopRate(0.5)
                    .comment(null)
                    .build();
            neoVbsConditionEntityRepository.save(condition);
            List<NeoVbsMultiBacktestRow> tradeHistory = backtest(condition, range);

            neoVbsTradeEntities = convert(condition, tradeHistory);
            condition.setNeoVbsTradeEntityList(neoVbsTradeEntities);
            neoVbsTradeEntityRepository.saveAll(neoVbsTradeEntities);

            neoVbsConditionEntities.add(condition);
        }
        return neoVbsConditionEntities;
    }

    /**
     * @param analysisMultiCondition 매매 분석 조건
     * @return 대상코인의 수익률 정보를 제공
     */
    private List<NeoVbsTradeReportItem> trading(AnalysisMultiCondition analysisMultiCondition) {
        List<NeoVbsConditionEntity> neoVbsConditionEntityList = neoVbsConditionEntityRepository.findAllById(analysisMultiCondition.getConditionIdSet());

        List<List<NeoVbsTradeEntity>> tradeList = neoVbsConditionEntityList.stream().map(NeoVbsConditionEntity::getNeoVbsTradeEntityList).collect(Collectors.toList());

        List<NeoVbsTradeEntity> allTrade = new ArrayList<>();
        for (List<NeoVbsTradeEntity> tradeHistory : tradeList) {
            List<NeoVbsTradeEntity> targetTradeHistory = tradeHistory.stream()
                    .filter(p -> analysisMultiCondition.getRange().isBetween(p.getTradeTimeKst()))
                    .collect(Collectors.toList());
            if (targetTradeHistory.isEmpty()) {
                continue;
            }

            List<NeoVbsTradeEntity> list = makePairTrade(targetTradeHistory);
            allTrade.addAll(list);
        }
        allTrade.sort(Comparator.comparing(NeoVbsTradeEntity::getTradeTimeKst));

        List<NeoVbsTradeReportItem> reportHistory = new ArrayList<>();

        Set<Integer> ids = analysisMultiCondition.getConditionIdSet();
        int allowBuyCount = ids.size();

        int buyCount = 0;
        double cash = analysisMultiCondition.getCash();

        // 코인명:매수가격
        Map<String, Double> buyCoinAmount = new HashMap<>();

        for (NeoVbsTradeEntity trade : allTrade) {

            NeoVbsTradeReportItem.NeoVbsTradeReportItemBuilder itemBuilder = NeoVbsTradeReportItem.builder();
            itemBuilder.neoVbsTradeEntity(trade);
            String market = trade.getVbsConditionEntity().getMarket();


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
    // TODO 중복 코드 제거 할 수 있을것 같음
    private List<NeoVbsTradeEntity> makePairTrade(List<NeoVbsTradeEntity> targetTradeHistory) {
        int skip = targetTradeHistory.get(0).getTradeType() == TradeType.BUY ? 0 : 1;
        int size = targetTradeHistory.size();
        int limit = targetTradeHistory.get(size - 1).getTradeType() == TradeType.SELL ? size - skip : size - skip - 1;
        return targetTradeHistory.stream()
                .skip(skip)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * @param condition    거래 조건
     * @param tradeHistory 거래 이력
     * @return 거래 내역 entity 변환
     */
    private List<NeoVbsTradeEntity> convert(NeoVbsConditionEntity condition, List<NeoVbsMultiBacktestRow> tradeHistory) {
        return tradeHistory.stream().map(p -> NeoVbsTradeEntity.builder()
                .vbsConditionEntity(condition)
                .tradeType(p.getTradeEvent())
                .highYield(p.getHighYield())
                .lowYield(p.getLowYield())
                .targetPrice(p.getTargetPrice())
                .yield(p.getRealYield())
                .unitPrice(p.getTradeEvent() == TradeType.BUY ? p.getBidPrice() : p.getAskPrice())
                .sellReason(p.getAskReason())
                .tradeTimeKst(p.getCandle().getCandleDateTimeKst())
                .targetPrice(p.getTargetPrice())
                .build()).collect(Collectors.toList());
    }

    /**
     * @param condition 조건
     * @param range     백테스트 범위(UTC 기준)
     * @return 거래 내역
     */
    private List<NeoVbsMultiBacktestRow> backtest(NeoVbsConditionEntity condition, DateRange range) {
        // key: market, value: 자산
        accountMap = new HashMap<>();

        Account cashAccount = new Account();
        cashAccount.setCurrency("KRW");
        cashAccount.setBalance(ApplicationUtil.toNumberString(CASH));
        accountMap.put("KRW", cashAccount);

        Account acc = new Account();
        String market = condition.getMarket();
        String[] tokens = market.split("-");
        acc.setUnitCurrency(tokens[0]);
        acc.setCurrency(tokens[1]);
        acc.setBalance("0");
        accountMap.put(market, acc);

        // Key: market, value: 시세 정보
        priceMap = new HashMap<>();
        targetPriceMap = new HashMap<>();

        injectionFieldValue(condition);
        tradeHistory = new ArrayList<>();

        LocalDateTime current = range.getFrom();
        LocalDateTime to = range.getTo();
        CandleDataProvider candleDataProvider = new CandleDataProvider(candleRepositoryCustom);

        initMock(candleDataProvider);

        int count = 0;
        while (current.isBefore(to) || current.equals(to)) {
            if (count == 1440 * 7) {
                log.info("clear: {}, {}, {}", condition.getMarket(), current, count);
                Mockito.reset(candleService, orderService, accountService, tradeEvent);
                initMock(candleDataProvider);
                count = 0;
            }

            candleDataProvider.setCurrentTime(current);
            CandleMinute candle = candleDataProvider.getCurrentCandle(condition.getMarket());
            if (candle == null) {
                current = current.plusMinutes(1);
                continue;
            }

            TradeResult tradeResult = TradeResult.builder()
                    .type("trade")
                    .code(candle.getMarket())
                    .tradePrice(candle.getTradePrice())
                    .tradeDate(candle.getCandleDateTimeUtc().toLocalDate())
                    .tradeTime(candle.getCandleDateTimeUtc().toLocalTime())
                    // 백테스트에서는 의미없는값
                    .timestamp(0L)
                    .prevClosingPrice(0)
                    .tradeVolume(0)
                    .build();

            neoVbsMultiService.tradeEvent(tradeResult);
            current = current.plusMinutes(1);
            count++;
        }

        Mockito.reset(candleService, orderService, accountService, tradeEvent);
        return tradeHistory;
    }

    private void injectionFieldValue(NeoVbsConditionEntity condition) {
        ReflectionTestUtils.setField(tradeCommonService, "coinByCandles", new HashMap<>());
        ReflectionTestUtils.setField(tradeCommonService, "assetHistoryRepository", this.assetHistoryRepository);
        ReflectionTestUtils.setField(tradeCommonService, "tradeRepository", this.tradeRepository);
        ReflectionTestUtils.setField(neoVbsMultiService, "tradeCommonService", this.tradeCommonService);
        ReflectionTestUtils.setField(neoVbsMultiService, "periodIdx", -1);

        NeoVbsMultiProperties properties = new NeoVbsMultiProperties();
        properties.setMarkets(Collections.singletonList(condition.getMarket()));
        properties.setMaxBuyCount(1);
        properties.setInvestRatio(0.99);
        properties.setLoseStopRate(condition.getLoseStopRate());
        properties.setPeriodType(condition.getTradePeriod());
        properties.setK(condition.getK());
        ReflectionTestUtils.setField(neoVbsMultiService, "properties", properties);

    }

    private void initMock(CandleDataProvider candleDataProvider) {
        when(candleService.getMinute(anyInt(), anyString()))
                .then((invocation) -> candleDataProvider.getCurrentCandle(invocation.getArgument(1)));

        when(candleService.getMinute(eq(15), anyString(), anyInt()))
                .then((invocation) -> candleDataProvider.beforeMinute(invocation.getArgument(1, String.class), PeriodType.PERIOD_15, invocation.getArgument(2, Integer.class)));
        when(candleService.getMinute(eq(30), anyString(), anyInt()))
                .then((invocation) -> candleDataProvider.beforeMinute(invocation.getArgument(1, String.class), PeriodType.PERIOD_30, invocation.getArgument(2, Integer.class)));
        when(candleService.getMinute(eq(60), anyString(), anyInt()))
                .then((invocation) -> candleDataProvider.beforeMinute(invocation.getArgument(1, String.class), PeriodType.PERIOD_60, invocation.getArgument(2, Integer.class)));
        when(candleService.getMinute(eq(240), anyString(), anyInt()))
                .then((invocation) -> candleDataProvider.beforeMinute(invocation.getArgument(1, String.class), PeriodType.PERIOD_240, invocation.getArgument(2, Integer.class)));

        when(candleService.getDay(anyString()))
                .then((invocation) -> {
                    List<CandleDay> candleDays = candleDataProvider.beforeDayCandle(invocation.getArgument(0, String.class), 2);
                    return candleDays.get(0);
                });
        when(candleService.getDay(anyString(), anyInt()))
                .then((invocation) -> candleDataProvider.beforeDayCandle(invocation.getArgument(0, String.class), invocation.getArgument(1, Integer.class)));

        // 현재 가지고있는 자산 조회
        when(accountService.getMyAccountBalance()).then((method) -> accountMap.entrySet().stream()
                .filter(e -> e.getValue().getBalanceValue() != 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));


        // 시세 체크
        doAnswer(invocation -> {
            Candle currentCandle = invocation.getArgument(0);
            priceMap.put(currentCandle.getMarket(), new CurrentPrice(currentCandle));
            return null;
            // TODO check 사용할수 있게 이벤트 적용
        }).when(tradeEvent).check(notNull());

        // 목표 가격
        doAnswer(invocation -> {
            String market = invocation.getArgument(0);
            double targetPrice = invocation.getArgument(1);
            targetPriceMap.put(market, targetPrice);
            return null;
        }).when(tradeEvent).setTargetPrice(notNull(), anyDouble());

        // 매수
        doAnswer(invocation -> {
            String market = invocation.getArgument(0, String.class);
            CurrentPrice currentPrice = priceMap.get(market);
            Candle candle = currentPrice.getCandle();
            NeoVbsMultiBacktestRow backtestRow = new NeoVbsMultiBacktestRow(depthCopy(candle));
            double tradePrice = invocation.getArgument(1);

            Account coinAccount = accountMap.get(market);
            coinAccount.setAvgBuyPrice(ApplicationUtil.toNumberString(tradePrice));
            double investAmount = invocation.getArgument(2);

            Account krwAccount = accountMap.get("KRW");
            double cash = Double.parseDouble(krwAccount.getBalance()) - investAmount;
            krwAccount.setBalance(ApplicationUtil.toNumberString(cash));

            String balance = ApplicationUtil.toNumberString(investAmount / tradePrice);
            coinAccount.setBalance(balance);

            backtestRow.setTradeEvent(TradeType.BUY);
            backtestRow.setBidPrice(tradePrice);
            backtestRow.setBuyAmount(investAmount);
            backtestRow.setBuyTotalAmount(getBuyTotalAmount(accountMap));
            backtestRow.setCash(cash);
            backtestRow.setTargetPrice(targetPriceMap.get(market));

            tradeHistory.add(backtestRow);

            return null;
        }).when(tradeEvent).bid(anyString(), anyDouble(), anyDouble());

        // 최고수익률
        doAnswer(invocation -> {
            this.highYield = invocation.getArgument(1, Double.class);
            return null;
        }).when(tradeEvent).highYield(anyString(), anyDouble());

        // 최저 수익률
        doAnswer(this::answer).when(tradeEvent).lowYield(anyString(), anyDouble());

        // 매도
        doAnswer(invocation -> {
            String market = invocation.getArgument(0, String.class);
            CurrentPrice currentPrice = priceMap.get(market);
            Candle candle = currentPrice.getCandle();

            NeoVbsMultiBacktestRow backtestRow = new NeoVbsMultiBacktestRow(depthCopy(candle));

            Account coinAccount = accountMap.get(market);
            backtestRow.setBidPrice(coinAccount.getAvgBuyPriceValue());
            backtestRow.setBuyAmount(coinAccount.getInvestCash());

            double tradePrice = invocation.getArgument(2);
            double balance = Double.parseDouble(coinAccount.getBalance());
            double askAmount = tradePrice * balance;

            Account krwAccount = accountMap.get("KRW");
            double totalCash = Double.parseDouble(krwAccount.getBalance()) + askAmount;
            krwAccount.setBalance(ApplicationUtil.toNumberString(totalCash));
            coinAccount.setBalance("0");
            coinAccount.setAvgBuyPrice(null);

            backtestRow.setBuyTotalAmount(getBuyTotalAmount(accountMap));
            backtestRow.setTradeEvent(TradeType.SELL);
            backtestRow.setAskPrice(tradePrice);
            backtestRow.setCash(krwAccount.getBalanceValue());
            backtestRow.setAskReason(invocation.getArgument(3));
            backtestRow.setHighYield(highYield);
            backtestRow.setLowYield(lowYield);

            tradeHistory.add(backtestRow);
            return null;
        }).when(tradeEvent).ask(anyString(), anyDouble(), anyDouble(), notNull());
    }

    /**
     * @param accountMap 코인(현금 포함) 계좌
     * @return 현재 투자한 코인 함
     */
    private double getBuyTotalAmount(Map<String, Account> accountMap) {
        return accountMap.entrySet().stream().filter(e -> !e.getKey().equals("KRW")).mapToDouble(e -> e.getValue().getInvestCash()).sum();
    }

    private Object answer(InvocationOnMock invocation) {
        this.lowYield = invocation.getArgument(1, Double.class);
        return null;
    }

    @RequiredArgsConstructor
    @Getter
    static class CurrentPrice {
        final Candle candle;
    }
}
