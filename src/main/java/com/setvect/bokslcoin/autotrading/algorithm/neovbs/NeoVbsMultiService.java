package com.setvect.bokslcoin.autotrading.algorithm.neovbs;

import com.setvect.bokslcoin.autotrading.algorithm.CoinTrading;
import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.common.TradeCommonParameter;
import com.setvect.bokslcoin.autotrading.algorithm.common.TradeCommonService;
import com.setvect.bokslcoin.autotrading.algorithm.common.TradeCommonUtil;
import com.setvect.bokslcoin.autotrading.algorithm.websocket.TradeResult;
import com.setvect.bokslcoin.autotrading.backtest.entity.PeriodType;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.OrderHistory;
import com.setvect.bokslcoin.autotrading.record.entity.AssetHistoryEntity;
import com.setvect.bokslcoin.autotrading.util.LimitedSizeQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NeoVbsMultiService implements CoinTrading {
    private final TradeCommonService tradeCommonService;
    private final NeoVbsMultiProperties properties;
    private final TradeEvent tradeEvent;
    /**
     * 마지막 체결 시세
     * (코인코드: 체결)
     */
    private final Map<String, TradeResult> currentTradeResult = new HashMap<>();
    private int periodIdx = -1;

    @Override
    public void tradeEvent(TradeResult tradeResult) {
        TradeResult beforeTradeResult = currentTradeResult.get(tradeResult.getCode());
        currentTradeResult.put(tradeResult.getCode(), tradeResult);
        PeriodType periodType = properties.getPeriodType();
        if (tradeCommonService.getCoinCandleSize() < properties.getMarkets().size()) {
            tradeCommonService.init(
                    TradeCommonParameter.builder()
                            .candleLoadCount(2)
                            .markets(properties.getMarkets())
                            .periodType(periodType)
                            .maxBuyCount(properties.getMaxBuyCount())
                            .investRatio(properties.getInvestRatio())
                            .build());
        }

        LocalDateTime nowUtc = tradeResult.getTradeDateTimeUtc();
        int currentPeriod = TradeCommonUtil.getCurrentPeriod(nowUtc, periodType);


        // 새로운 날짜면 매매 다시 초기화
        if (periodIdx != currentPeriod) {
            tradeEvent.newPeriod(tradeResult);
            tradeCommonService.loadAccount();
            List<AssetHistoryEntity> rateByCoin = tradeCommonService.saveAsset(tradeResult.getTradeDateTimeUtc());
            sendCurrentStatus(rateByCoin);
            tradeCommonService.clearTradeCompleteOfPeriod();
            periodIdx = currentPeriod;
        }

        LimitedSizeQueue<Candle> candles = tradeCommonService.getCandles(tradeResult.getCode());
        // 맨 앞에 가장 최근
        Candle newestCandle = candles.get(0);

        LocalDateTime currentDateTimeUtc = periodType.fitDateTime(tradeResult.getTradeDateTimeUtc());
        LocalDateTime candleDateTimeUtc = periodType.fitDateTime(newestCandle.getCandleDateTimeUtc());

        boolean newPeriod = false;
        if (candleDateTimeUtc.equals(currentDateTimeUtc)) {
            newestCandle.change(tradeResult);

            // 같은 주기에서 이전과 같은 체결값이면 이후 처리는 하지 않음
            if (beforeTradeResult != null && beforeTradeResult.getTradePrice() == tradeResult.getTradePrice()) {
                return;
            }
        } else {
            newestCandle = new Candle(tradeResult);
            // 최근 캔들이 맨 앞에 있기 때문에 index 0에 넣음
            candles.add(0, newestCandle);
            newPeriod = true;
        }
        tradeEvent.check(newestCandle);

        String market = tradeResult.getCode();
        if (isBuyable(market)) {
            tradeCommonService.doBid(market);
        } else if (isSellable(market, newPeriod)) {
            tradeCommonService.doAsk(market);
        }
    }

    /**
     * @param market 매수 대상 코인
     * @return true 매수 조건 만족
     */
    private boolean isBuyable(String market) {
        // 해당 주기에서 매도한 종목은 다시 매수 하지 않음
        if (tradeCommonService.existCoin(market)) {
            return false;
        }

        List<Candle> candleList = tradeCommonService.getCandles(market);
        Candle currentCandle = candleList.get(0);
        double targetValue = getTargetValue(market);
        tradeEvent.setTargetPrice(market, targetValue);

        //(장기이평 + 장기이평 * 상승매수률) <= 단기이평
        boolean isBuy = targetValue <= currentCandle.getTradePrice();
        double cash = tradeCommonService.getBuyCash();
        if (!isBuy || !(cash >= TradeCommonUtil.MINIMUM_BUY_CASH)) {
            return false;
        }
        // 매수 직전 주문 요청 이력 확인
        tradeCommonService.loadOrderWait();
        OrderHistory orderHistory = tradeCommonService.getOrderHistory(market);
        if (orderHistory != null) {
            log.info("{} 매수 주문요청  상태임: {}", market, orderHistory);
            return false;
        }
        // 매수 대기 처리 여부 확인을 위해 계좌 내역 한번더 조회
        tradeCommonService.loadAccount();
        if (tradeCommonService.existCoin(market)) {
            log.warn("[{}] 매수 안함. 이미 매수한 종목", market);
            return false;
        }
        return true;
    }


    /**
     * @param market    매도 대상 코인
     * @param newCandle 매수 후 새로운 주기 진입 여부
     * @return true 매도 조건 만족
     */
    // TODO 실재 운영에서 새로운 주기 기록을 판단할 수 없다. 언제 샀다는 걸 어딘가에 기록해야 된다.
    private boolean isSellable(String market, boolean newCandle) {
        Account account = tradeCommonService.getAccount(market);
        if (account == null) {
            return false;
        }

        tradeCommonService.emitHighMinYield(market);

        if (!newCandle) {
            return false;
        }

        // 매도 직전 주문 요청 이력 확인
        tradeCommonService.loadOrderWait();
        OrderHistory orderHistory = tradeCommonService.getOrderHistory(market);
        if (orderHistory != null) {
            log.info("{} 매도 주문요청  상태임: {}", market, orderHistory);
            return false;
        }
        return true;

    }

    /**
     * @param market 종목코드
     * @return 목표가
     */
    private double getTargetValue(String market) {
        List<Candle> candleList = tradeCommonService.getCandles(market);
        Candle currentCandle = candleList.get(0);
        Candle previousCandle = candleList.get(1);

        return (previousCandle.getHighPrice() - previousCandle.getLowPrice()) * properties.getK() + currentCandle.getOpeningPrice();
    }

    /**
     * 현재 시세정보와 투자 수익률 슬렉으로 전달
     *
     * @param rateByCoin 코인 투자 수익률
     */
    private void sendCurrentStatus(List<AssetHistoryEntity> rateByCoin) {
        Map<String, AssetHistoryEntity> coinByAsset = rateByCoin.stream().collect(Collectors.toMap(AssetHistoryEntity::getCurrency, Function.identity()));

        String priceMessage = tradeCommonService.getCoinByCandles().values().stream().map(candleList -> {
            Candle candle = candleList.get(0);
            String market = candle.getMarket();
            double targetValue = getTargetValue(market);

            TradeResult tradeResult = currentTradeResult.get(market);
            String dayYield = Optional.ofNullable(tradeResult).map(p -> String.format("%.2f%%", p.getYieldDay() * 100)).orElse("X");
            String message = String.format("[%s] %,.0f, %s, %,.0f", TradeCommonUtil.removeKrw(market), targetValue, dayYield, candle.getTradePrice());
            AssetHistoryEntity asset = coinByAsset.get(market);
            return message + Optional.ofNullable(asset).map(p -> String.format(", %.2f%%", p.getYield() * 100)).orElse(", _");
        }).collect(Collectors.joining("\n"));

        tradeCommonService.sendCurrentStatus(rateByCoin, currentTradeResult, priceMessage);
    }
}
