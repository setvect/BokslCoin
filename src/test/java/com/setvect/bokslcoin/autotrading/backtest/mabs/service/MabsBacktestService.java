package com.setvect.bokslcoin.autotrading.backtest.mabs.service;

import com.setvect.bokslcoin.autotrading.algorithm.common.TradeCommonService;
import com.setvect.bokslcoin.autotrading.algorithm.mabs.MabsMultiProperties;
import com.setvect.bokslcoin.autotrading.algorithm.websocket.TradeResult;
import com.setvect.bokslcoin.autotrading.backtest.common.BacktestHelperComponent;
import com.setvect.bokslcoin.autotrading.backtest.common.CandleDataProvider;
import com.setvect.bokslcoin.autotrading.backtest.common.mock.*;
import com.setvect.bokslcoin.autotrading.backtest.common.model.AnalysisMultiCondition;
import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.entity.mabs.MabsTradeEntity;
import com.setvect.bokslcoin.autotrading.backtest.mabs.mock.MockMabsMultiProperties;
import com.setvect.bokslcoin.autotrading.backtest.mabs.mock.MockMabsMultiService;
import com.setvect.bokslcoin.autotrading.backtest.mabs.mock.MockMabsTradeEvent;
import com.setvect.bokslcoin.autotrading.backtest.mabs.model.MabsMultiBacktestRow;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepositoryCustom;
import com.setvect.bokslcoin.autotrading.backtest.repository.MabsConditionEntityRepository;
import com.setvect.bokslcoin.autotrading.backtest.repository.MabsTradeEntityRepository;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.record.entity.TradeType;
import com.setvect.bokslcoin.autotrading.record.repository.AssetHistoryRepository;
import com.setvect.bokslcoin.autotrading.record.repository.TradeRepository;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
public class MabsBacktestService {
    private final SlackMessageService slackMessageService = new MockSlackMessageService();
    private final MockMabsTradeEvent tradeEvent = new MockMabsTradeEvent(slackMessageService);
    private final MockAccountService accountService = new MockAccountService();
    private final MockOrderService orderService = new MockOrderService(new MockAccessTokenMaker(), new MockConnectionInfo());
    private final MockCandleService candleService = new MockCandleService(new MockConnectionInfo());
    @Autowired
    private MabsConditionEntityRepository mabsConditionEntityRepository;
    @Autowired
    private MabsTradeEntityRepository mabsTradeEntityRepository;
    @Autowired
    private CandleRepositoryCustom candleRepositoryCustom;
    @Autowired
    private AssetHistoryRepository assetHistoryRepository;
    @Autowired
    private TradeRepository tradeRepository;
    @Autowired
    private BacktestHelperComponent backtestHelperService;
    @Autowired
    private MakeBacktestReportService makeBacktestReportService;

    private TradeCommonService tradeCommonService;
    private MockMabsMultiService mabsMultiService;

    private void initMock() {
        tradeCommonService = new MockTradeCommonService(tradeEvent, accountService, orderService, candleService, tradeRepository, slackMessageService, assetHistoryRepository);
        mabsMultiService = new MockMabsMultiService(tradeCommonService, tradeEvent, new MockMabsMultiProperties());
    }

    public void backtest(List<MabsConditionEntity> mabsConditionEntities, DateRange range, AnalysisMultiCondition.AnalysisMultiConditionBuilder analysisMultiConditionBuilder) {
        boolean saveDb = false;
        initMock();

        try {
            mabsConditionEntities = backtestSub(mabsConditionEntities, range);
            List<Integer> conditionSeqList = getConditionSeqList(mabsConditionEntities);

            AnalysisMultiCondition analysisMultiCondition = analysisMultiConditionBuilder.conditionIdSet(new HashSet<>(conditionSeqList)).build();

            makeBacktestReportService.makeReport(analysisMultiCondition);
        } finally {
            if (!saveDb && CollectionUtils.isNotEmpty(mabsConditionEntities)) {
                List<Integer> conditionSeqList = getConditionSeqList(mabsConditionEntities);
                // 결과를 삭제함
                // TODO 너무 무식한 방법이다. @Transactional를 사용해야 되는데 사용하면 속도가 매우 느리다. 해결해야됨
                mabsTradeEntityRepository.deleteTradeByConditionId(conditionSeqList);
                mabsConditionEntityRepository.deleteAll(mabsConditionEntities);
            }
        }
    }


    public void backtestIncremental(List<Integer> conditionSeqList) {
        initMock();
        // 완전한 거래(매수-매도 쌍)를 만들기 위해 마지막 거래가 매수인경우 거래 내역 삭제
        deleteLastBuy(conditionSeqList);

        List<MabsConditionEntity> conditionEntityList = mabsConditionEntityRepository.findAllById(conditionSeqList);

        for (MabsConditionEntity condition : conditionEntityList) {
            log.info("{}, {}, {}_{} 시작", condition.getMarket(), condition.getTradePeriod(), condition.getLongPeriod(), condition.getShortPeriod());
            List<MabsTradeEntity> tradeList = mabsTradeEntityRepository.findByCondition(condition.getConditionSeq());

            LocalDateTime start = backtestHelperService.makeBaseStart(condition.getMarket(), condition.getTradePeriod(), condition.getLongPeriod() + 1);
            if (!tradeList.isEmpty()) {
                MabsTradeEntity lastTrade = tradeList.get(tradeList.size() - 1);
                checkLastSell(lastTrade);
                start = lastTrade.getTradeTimeKst();
            }
            DateRange range = new DateRange(start, LocalDateTime.now());
            List<MabsMultiBacktestRow> tradeHistory = backtestSub(condition, range);

            List<MabsTradeEntity> mabsTradeEntities = convert(condition, tradeHistory);
            log.info("[{}] save. range: {}, trade Count: {}", condition.getMarket(), range, mabsTradeEntities.size());
            mabsTradeEntityRepository.saveAll(mabsTradeEntities);
        }
    }


    @NotNull
    List<Integer> getConditionSeqList(List<MabsConditionEntity> mabsConditionEntities) {
        return mabsConditionEntities.stream()
                .map(MabsConditionEntity::getConditionSeq)
                .collect(Collectors.toList());
    }

    public List<MabsConditionEntity> backtestSub(List<MabsConditionEntity> mabsConditionEntities, DateRange range) {
        for (MabsConditionEntity condition : mabsConditionEntities) {
            mabsConditionEntityRepository.save(condition);
            List<MabsMultiBacktestRow> tradeHistory = backtestSub(condition, range);

            List<MabsTradeEntity> mabsTradeEntities = convert(condition, tradeHistory);
            mabsTradeEntityRepository.saveAll(mabsTradeEntities);
        }
        return mabsConditionEntities;
    }


    void checkLastSell(MabsTradeEntity lastTrade) {
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
        List<MabsConditionEntity> conditionEntityList = mabsConditionEntityRepository.findAllById(conditionSeqList);

        for (MabsConditionEntity condition : conditionEntityList) {
            List<MabsTradeEntity> tradeList = mabsTradeEntityRepository.findByCondition(condition.getConditionSeq());
            if (tradeList.isEmpty()) {
                continue;
            }
            MabsTradeEntity lastTrade = tradeList.get(tradeList.size() - 1);
            log.info("count: {}, last: {} -> {} ", tradeList.size(), lastTrade.getTradeType(), lastTrade.getTradeTimeKst());
            if (lastTrade.getTradeType() == TradeType.SELL) {
                continue;
            }

            log.info("Delete Last Buy: {} {}", lastTrade.getTradeSeq(), lastTrade.getTradeTimeKst());
            mabsTradeEntityRepository.deleteById(lastTrade.getTradeSeq());
        }
    }

    /**
     * @param condition    거래 조건
     * @param tradeHistory 거래 이력
     * @return 거래 내역 entity 변환
     */
    List<MabsTradeEntity> convert(MabsConditionEntity condition, List<MabsMultiBacktestRow> tradeHistory) {
        return tradeHistory.stream().map(p -> MabsTradeEntity.builder()
                .conditionEntity(condition)
                .tradeType(p.getTradeEvent())
                .highYield(p.getHighYield())
                .lowYield(p.getLowYield())
                .maShort(p.getMaShort())
                .maLong(p.getMaLong())
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
    List<MabsMultiBacktestRow> backtestSub(MabsConditionEntity condition, DateRange range) {
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

        // Key: market, value: 시세 정보
        Map<String, CurrentPrice> priceMap = new HashMap<>();

        injectionFieldValue(condition);
        List<MabsMultiBacktestRow> tradeHistory = new ArrayList<>();

        tradeEvent.setPriceMap(priceMap);
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

            mabsMultiService.tradeEvent(tradeResult);
            current = current.plusMinutes(1);
        }
        return tradeHistory;
    }

    private void injectionFieldValue(MabsConditionEntity condition) {
        ReflectionTestUtils.setField(tradeCommonService, "coinByCandles", new HashMap<>());
        ReflectionTestUtils.setField(tradeCommonService, "assetHistoryRepository", this.assetHistoryRepository);
        ReflectionTestUtils.setField(tradeCommonService, "tradeRepository", this.tradeRepository);
        ReflectionTestUtils.setField(mabsMultiService, "tradeCommonService", this.tradeCommonService);
        ReflectionTestUtils.setField(mabsMultiService, "periodIdx", -1);

        MabsMultiProperties properties = mabsMultiService.getProperties();
        properties.setMarkets(Collections.singletonList(condition.getMarket()));
        properties.setMaxBuyCount(1);
        properties.setInvestRatio(0.99);
        properties.setUpBuyRate(condition.getUpBuyRate());
        properties.setLoseStopRate(condition.getLoseStopRate());
        properties.setDownSellRate(condition.getDownSellRate());
        properties.setPeriodType(condition.getTradePeriod());
        properties.setShortPeriod(condition.getShortPeriod());
        properties.setLongPeriod(condition.getLongPeriod());
        properties.setNewMasBuy(true);

        ReflectionTestUtils.setField(mabsMultiService, "properties", properties);
    }

    @RequiredArgsConstructor
    @Getter
    public static class CurrentPrice {
        final Candle candle;
        final double maShort;
        final double maLong;
    }
}
