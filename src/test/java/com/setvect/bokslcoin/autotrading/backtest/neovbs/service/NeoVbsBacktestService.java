package com.setvect.bokslcoin.autotrading.backtest.neovbs.service;

import com.setvect.bokslcoin.autotrading.algorithm.common.TradeCommonService;
import com.setvect.bokslcoin.autotrading.algorithm.neovbs.NeoVbsMultiProperties;
import com.setvect.bokslcoin.autotrading.algorithm.websocket.TradeResult;
import com.setvect.bokslcoin.autotrading.backtest.common.BacktestHelperComponent;
import com.setvect.bokslcoin.autotrading.backtest.common.CandleDataProvider;
import com.setvect.bokslcoin.autotrading.backtest.common.mock.*;
import com.setvect.bokslcoin.autotrading.backtest.common.model.AnalysisMultiCondition;
import com.setvect.bokslcoin.autotrading.backtest.entity.neovbs.NeoVbsConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.neovbs.NeoVbsTradeEntity;
import com.setvect.bokslcoin.autotrading.backtest.neovbs.mock.MockNeoVbsMultiProperties;
import com.setvect.bokslcoin.autotrading.backtest.neovbs.mock.MockNeoVbsMultiService;
import com.setvect.bokslcoin.autotrading.backtest.neovbs.mock.MockNeoVbsTradeEvent;
import com.setvect.bokslcoin.autotrading.backtest.neovbs.model.NeoVbsMultiBacktestRow;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepositoryCustom;
import com.setvect.bokslcoin.autotrading.backtest.repository.NeoVbsConditionEntityRepository;
import com.setvect.bokslcoin.autotrading.backtest.repository.NeoVbsTradeEntityRepository;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import com.setvect.bokslcoin.autotrading.record.repository.AssetHistoryRepository;
import com.setvect.bokslcoin.autotrading.record.repository.TradeRepository;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NeoVbsBacktestService {
    private final SlackMessageService slackMessageService = new MockSlackMessageService();
    private final MockNeoVbsTradeEvent tradeEvent = new MockNeoVbsTradeEvent(slackMessageService);
    private final MockAccountService accountService = new MockAccountService();
    private final MockOrderService orderService = new MockOrderService(new MockAccessTokenMaker(), new MockConnectionInfo());
    private final MockCandleService candleService = new MockCandleService(new MockConnectionInfo());
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
    @Autowired
    private BacktestHelperComponent backtestHelperService;
    @Autowired
    private NeoVbsMakeBacktestReportService makeBacktestReportService;

    private TradeCommonService tradeCommonService;
    private MockNeoVbsMultiService neoVbsMultiService;

    private void initMock() {
        tradeCommonService = new MockTradeCommonService(tradeEvent, accountService, orderService, candleService, tradeRepository, slackMessageService, assetHistoryRepository);
        neoVbsMultiService = new MockNeoVbsMultiService(tradeCommonService, tradeEvent, new MockNeoVbsMultiProperties());
    }

    public void backtest(List<NeoVbsConditionEntity> neoVbsConditionEntities, DateRange range, AnalysisMultiCondition.AnalysisMultiConditionBuilder analysisMultiConditionBuilder) {
        boolean saveDb = false;
        initMock();

        try {
            neoVbsConditionEntities = backtestSub(neoVbsConditionEntities, range);
            List<Integer> conditionSeqList = getConditionSeqList(neoVbsConditionEntities);

            AnalysisMultiCondition analysisMultiCondition = analysisMultiConditionBuilder.conditionIdSet(new HashSet<>(conditionSeqList)).build();

            makeBacktestReportService.makeReport(analysisMultiCondition);
        } finally {
            if (!saveDb && CollectionUtils.isNotEmpty(neoVbsConditionEntities)) {
                List<Integer> conditionSeqList = getConditionSeqList(neoVbsConditionEntities);
                // 결과를 삭제함
                // TODO 너무 무식한 방법이다. @Transactional를 사용해야 되는데 사용하면 속도가 매우 느리다. 해결해야됨
                neoVbsTradeEntityRepository.deleteTradeByConditionId(conditionSeqList);
                neoVbsConditionEntityRepository.deleteAll(neoVbsConditionEntities);
            }
        }
    }


    public void backtestIncremental(List<Integer> conditionSeqList) {
        initMock();
        // 완전한 거래(매수-매도 쌍)를 만들기 위해 마지막 거래가 매수인경우 거래 내역 삭제
        deleteLastBuy(conditionSeqList);

        List<NeoVbsConditionEntity> conditionEntityList = neoVbsConditionEntityRepository.findAllById(conditionSeqList);

        for (NeoVbsConditionEntity condition : conditionEntityList) {
            log.info("{}, {}, {}, {}, {} 시작", condition.getMarket(), condition.getTradePeriod(), condition.getK(), condition.getLoseStopRate(), condition.getTrailingLossStopRate());
            List<NeoVbsTradeEntity> tradeList = neoVbsTradeEntityRepository.findByCondition(condition.getConditionSeq());

            LocalDateTime start = backtestHelperService.makeBaseStart(condition.getMarket(), condition.getTradePeriod(), 1);
            if (!tradeList.isEmpty()) {
                NeoVbsTradeEntity lastTrade = tradeList.get(tradeList.size() - 1);
                checkLastSell(lastTrade);
                start = lastTrade.getTradeTimeKst();
            }
            DateRange range = new DateRange(start, LocalDateTime.now());
            List<NeoVbsMultiBacktestRow> tradeHistory = backtestSub(condition, range);

            List<NeoVbsTradeEntity> neoVbsTradeEntities = convert(condition, tradeHistory);
            log.info("[{}] save. range: {}, trade Count: {}", condition.getMarket(), range, neoVbsTradeEntities.size());
            neoVbsTradeEntityRepository.saveAll(neoVbsTradeEntities);
        }
    }


    @NotNull
    List<Integer> getConditionSeqList(List<NeoVbsConditionEntity> neoVbsConditionEntities) {
        return neoVbsConditionEntities.stream()
                .map(NeoVbsConditionEntity::getConditionSeq)
                .collect(Collectors.toList());
    }

    public List<NeoVbsConditionEntity> backtestSub(List<NeoVbsConditionEntity> neoVbsConditionEntities, DateRange range) {
        for (NeoVbsConditionEntity condition : neoVbsConditionEntities) {
            neoVbsConditionEntityRepository.save(condition);
            List<NeoVbsMultiBacktestRow> tradeHistory = backtestSub(condition, range);

            List<NeoVbsTradeEntity> neoVbsTradeEntities = convert(condition, tradeHistory);
            neoVbsTradeEntityRepository.saveAll(neoVbsTradeEntities);
        }
        return neoVbsConditionEntities;
    }


    void checkLastSell(NeoVbsTradeEntity lastTrade) {
        if (lastTrade.getTradeType() == TradeType.BUY) {
            throw new RuntimeException(String.format("마지막 거래가 BUY인 항목이 있음. tradeSeq: %s", lastTrade.getTradeSeq()));
        }
    }

    /**
     * 거래 내역의 마지막이 매수인경우 해당 거래를 삭제
     *
     * @param conditionSeqList 거래 조건 일련번호
     */
    void deleteLastBuy(List<Integer> conditionSeqList) {
        List<NeoVbsConditionEntity> conditionEntityList = neoVbsConditionEntityRepository.findAllById(conditionSeqList);

        for (NeoVbsConditionEntity condition : conditionEntityList) {
            List<NeoVbsTradeEntity> tradeList = neoVbsTradeEntityRepository.findByCondition(condition.getConditionSeq());
            if (tradeList.isEmpty()) {
                continue;
            }
            NeoVbsTradeEntity lastTrade = tradeList.get(tradeList.size() - 1);
            log.info("count: {}, last: {} -> {} ", tradeList.size(), lastTrade.getTradeType(), lastTrade.getTradeTimeKst());
            if (lastTrade.getTradeType() == TradeType.SELL) {
                continue;
            }

            log.info("Delete Last Buy: {} {}", lastTrade.getTradeSeq(), lastTrade.getTradeTimeKst());
            neoVbsTradeEntityRepository.deleteById(lastTrade.getTradeSeq());
        }
    }

    /**
     * @param condition    거래 조건
     * @param tradeHistory 거래 이력
     * @return 거래 내역 entity 변환
     */
    List<NeoVbsTradeEntity> convert(NeoVbsConditionEntity condition, List<NeoVbsMultiBacktestRow> tradeHistory) {
        return tradeHistory.stream().map(p -> NeoVbsTradeEntity.builder()
                .conditionEntity(condition)
                .tradeType(p.getTradeEvent())
                .targetPrice(p.getTargetPrice())
                .highYield(p.getHighYield())
                .lowYield(p.getLowYield())
                .yield(p.getRealYield())
                .unitPrice(p.getTradeEvent() == TradeType.BUY ? p.getBidPrice() : p.getAskPrice())
                .sellReason(p.getAskReason())
                .tradeTimeKst(p.getCandle().getCandleDateTimeKst())
                .build()).collect(Collectors.toList());
    }

    /**
     * @param condition 조건
     * @param range     백테스트 범위(UTC 기준)
     * @return 거래 내역
     */
    List<NeoVbsMultiBacktestRow> backtestSub(NeoVbsConditionEntity condition, DateRange range) {
        // key: market, value: 자산
        Map<String, Account> accountMap = new HashMap<>();

        Account cashAccount = new Account();
        cashAccount.setCurrency("KRW");
        cashAccount.setBalance(ApplicationUtil.toNumberString(10_000_000));
        accountMap.put("KRW", cashAccount);

        Account acc = new Account();
        String market = condition.getMarket();
        String[] tokens = market.split("-");
        acc.setUnitCurrency(tokens[0]);
        acc.setCurrency(tokens[1]);
        acc.setBalance("0");
        accountMap.put(market, acc);


        injectionFieldValue(condition);
        List<NeoVbsMultiBacktestRow> tradeHistory = new ArrayList<>();

        tradeEvent.setAccountMap(accountMap);
        tradeEvent.setTradeHistory(tradeHistory);

        accountService.setAccountMap(accountMap);

        LocalDateTime current = range.getFrom();
        LocalDateTime to = range.getTo();
        CandleDataProvider candleDataProvider = new CandleDataProvider(candleRepositoryCustom);

        candleService.setCandleDataProvider(candleDataProvider);

        while (current.isBefore(to) || current.equals(to)) {
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
        }
        return tradeHistory;
    }

    private void injectionFieldValue(NeoVbsConditionEntity condition) {
        ReflectionTestUtils.setField(tradeCommonService, "coinByCandles", new HashMap<>());
        ReflectionTestUtils.setField(tradeCommonService, "assetHistoryRepository", this.assetHistoryRepository);
        ReflectionTestUtils.setField(tradeCommonService, "tradeRepository", this.tradeRepository);
        ReflectionTestUtils.setField(neoVbsMultiService, "tradeCommonService", this.tradeCommonService);
        ReflectionTestUtils.setField(neoVbsMultiService, "periodIdx", -1);

        NeoVbsMultiProperties properties = neoVbsMultiService.getProperties();
        properties.setMarkets(Collections.singletonList(condition.getMarket()));
        properties.setMaxBuyCount(1);
        properties.setInvestRatio(0.99);
        properties.setLoseStopRate(condition.getLoseStopRate());
        properties.setPeriodType(condition.getTradePeriod());
        properties.setK(condition.getK());
        properties.setGainStopRate(condition.getTrailingStopEnterRate());
        properties.setTrailingStopRate(condition.getTrailingLossStopRate());

        ReflectionTestUtils.setField(neoVbsMultiService, "properties", properties);
    }
}
