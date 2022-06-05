package com.setvect.bokslcoin.autotrading.algorithm.mabs;

import com.setvect.bokslcoin.autotrading.algorithm.CoinTrading;
import com.setvect.bokslcoin.autotrading.algorithm.CommonTradeHelper;
import com.setvect.bokslcoin.autotrading.algorithm.TradeEvent;
import com.setvect.bokslcoin.autotrading.algorithm.common.TradeCommonParameter;
import com.setvect.bokslcoin.autotrading.algorithm.common.TradeCommonService;
import com.setvect.bokslcoin.autotrading.algorithm.common.TradeCommonUtil;
import com.setvect.bokslcoin.autotrading.algorithm.websocket.TradeResult;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.OrderHistory;
import com.setvect.bokslcoin.autotrading.record.entity.AssetHistoryEntity;
import com.setvect.bokslcoin.autotrading.util.MathUtil;
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

/**
 * 이평선 돌파 전략 + 멀티 코인
 * 코인별 동일한 현금 비율로 매매를 수행한다.
 */
@Service("mabsMulti")
@RequiredArgsConstructor
@Slf4j
public class MabsMultiService implements CoinTrading {
    private final TradeCommonService tradeCommonService;
    private final TradeEvent tradeEvent;
    private final MabsMultiProperties properties;


    private int periodIdx = -1;

    /**
     * 마지막 체결 시세
     * (코인코드: 체결)
     */
    private final Map<String, TradeResult> currentTradeResult = new HashMap<>();


    /**
     * 본 메소드를 호출하는 쪽에서는 스레드 안전성을 보장해야됨
     *
     * @param tradeResult 체결 현황
     */
    @Override
    public void tradeEvent(TradeResult tradeResult) {
        TradeResult beforeTradeResult = currentTradeResult.get(tradeResult.getCode());
        currentTradeResult.put(tradeResult.getCode(), tradeResult);

        if (tradeCommonService.getCoinCandleSize() < properties.getMarkets().size()) {
            tradeCommonService.init(
                    TradeCommonParameter.builder()
                            .candleLoadCount(properties.getLongPeriod() + 1)
                            .markets(properties.getMarkets())
                            .periodType(properties.getPeriodType())
                            .maxBuyCount(properties.getMaxBuyCount())
                            .investRatio(properties.getInvestRatio())
                            .build());
        }

        LocalDateTime nowUtc = tradeResult.getTradeDateTimeUtc();
        int currentPeriod = TradeCommonUtil.getCurrentPeriod(nowUtc, properties.getPeriodType());


        // 새로운 날짜면 매매 다시 초기화
        if (periodIdx != currentPeriod) {
            tradeEvent.newPeriod(tradeResult);
            tradeCommonService.loadAccount();
            List<AssetHistoryEntity> rateByCoin = tradeCommonService.saveAsset(tradeResult.getTradeDateTimeKst());
            sendCurrentStatus(rateByCoin);
            tradeCommonService.clearTradeCompleteOfPeriod();
            periodIdx = currentPeriod;
        }

        String market = tradeResult.getCode();
        List<Candle> candles = tradeCommonService.getCandles(market);
        if (candles == null) {
            log.warn(String.format("%s 설정에 없는 시세데이타가 조회 되었습니다.", market));
            return;
        }

        // 맨 앞에 가장 최근
        Candle newestCandle = candles.get(0);
        LocalDateTime tradeDateTimeKst = properties.getPeriodType().fitDateTime(tradeResult.getTradeDateTimeKst());
        LocalDateTime candleDateTimeKst = properties.getPeriodType().fitDateTime(newestCandle.getCandleDateTimeKst());

        if (candleDateTimeKst.equals(tradeDateTimeKst)) {
            newestCandle.change(tradeResult);
            // 같은 주기에서 이전과 같은 체결값이면 이후 처리는 하지 않음
            if (beforeTradeResult != null && beforeTradeResult.getTradePrice() == tradeResult.getTradePrice()) {
                return;
            }
        } else {
            newestCandle = new Candle(tradeResult);
            // 최근 캔들이 맨 앞에 있기 때문에 index 0에 넣음
            candles.add(0, newestCandle);
        }

        double maShort = CommonTradeHelper.getMa(candles, properties.getShortPeriod());
        double maLong = CommonTradeHelper.getMa(candles, properties.getLongPeriod());
        tradeEvent.check(newestCandle, maShort, maLong);

        logCurrentPrice(tradeResult, maShort, maLong);
        tradeCommonService.checkStatus(currentTradeResult);

        if (isBuyable(market)) {
            tradeCommonService.doBid(market);
        } else if (isSellable(market)) {
            tradeCommonService.doAsk(market);
        }
    }

    /**
     * 현재 시세정보와 투자 수익률 슬렉으로 전달
     *
     * @param rateByCoin 코인 투자 수익률
     */
    private void sendCurrentStatus(List<AssetHistoryEntity> rateByCoin) {
        Map<String, AssetHistoryEntity> coinByAsset = rateByCoin.stream().collect(Collectors.toMap(AssetHistoryEntity::getCurrency, Function.identity()));

        String priceMessage = tradeCommonService.getCoinByCandles().values().stream().map(candleList -> {
            double maShort = CommonTradeHelper.getMa(candleList, properties.getShortPeriod());
            double maLong = CommonTradeHelper.getMa(candleList, properties.getLongPeriod());
            Candle candle = candleList.get(0);

            String market = candle.getMarket();
            TradeResult tradeResult = currentTradeResult.get(market);
            String dayYield = Optional.ofNullable(tradeResult).map(p -> String.format("%.2f%%", p.getYieldDay() * 100)).orElse("X");
            String message = String.format("[%s] %.2f%%, %s, %,.0f", TradeCommonUtil.removeKrw(market), MathUtil.getYield(maShort, maLong) * 100, dayYield, candle.getTradePrice());
            AssetHistoryEntity asset = coinByAsset.get(market);
            return message + Optional.ofNullable(asset).map(p -> String.format(", %.2f%%", p.getYield() * 100)).orElse(", _");
        }).collect(Collectors.joining("\n"));

        tradeCommonService.sendCurrentStatus(rateByCoin, currentTradeResult, priceMessage);
    }


    /**
     * @param market 매수 대상 코인
     * @return true 매수 조건 만족
     */
    private boolean isBuyable(String market) {
        // 해당 주기에서 매도한 종목은 다시 매수 하지 않음
        if (tradeCommonService.existTradeCompleteOfPeriod(market)) {
            return false;
        }
        if (tradeCommonService.existCoin(market)) {
            return false;
        }

        List<Candle> candleList = tradeCommonService.getCandles(market);
        double maShort = CommonTradeHelper.getMa(candleList, properties.getShortPeriod());
        double maLong = CommonTradeHelper.getMa(candleList, properties.getLongPeriod());

        double buyTargetPrice = maLong + maLong * properties.getUpBuyRate();

        //(장기이평 + 장기이평 * 상승매수률) <= 단기이평
        boolean isBuy = buyTargetPrice <= maShort;
        double cash = tradeCommonService.getBuyCash();
        if (!isBuy || !(cash >= TradeCommonUtil.MINIMUM_BUY_CASH)) {
            return false;
        }
        // 직전 이동평균을 감지해 새롭게 돌파 했을 때만 매수
        boolean isBeforeBuy = isBeforeBuy(candleList);
        Candle candle = candleList.get(0);
        if (isBeforeBuy && properties.isNewMasBuy()) {
            log.debug("[{}] 매수 안함. 새롭게 이동평균을 돌파할 때만 매수합니다.", candle.getMarket());
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
            log.warn("[{}] 매수 안함. 이미 매수한 종목", candle.getMarket());
            return false;
        }
        return true;
    }

    /**
     * @param market 매도 대상 코인
     * @return true 매도 조건 만족
     */
    private boolean isSellable(String market) {
        Account account = tradeCommonService.getAccount(market);
        if (account == null) {
            return false;
        }

        tradeCommonService.emitHighMinYield(market);
        List<Candle> candleList = tradeCommonService.getCandles(market);

        double maShort = CommonTradeHelper.getMa(candleList, properties.getShortPeriod());
        double maLong = CommonTradeHelper.getMa(candleList, properties.getLongPeriod());

        Candle candle = candleList.get(0);
        // 장기이평 >= (단기이평 + 단기이평 * 하락매도률)
        double yield = TradeCommonUtil.getYield(candle, account);
        double sellTargetPrice = maShort + maShort * properties.getDownSellRate();
        boolean isSell = maLong >= sellTargetPrice;
        boolean isLossStop = properties.getLoseStopRate() < -yield;
        if (!(isSell || isLossStop)) {
            // 매도 하지 않음
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
     * 10초마다 코인별 현재 가격 출력
     */
    private void logCurrentPrice(TradeResult tradeResult, double maShort, double maLong) {
        String message = String.format("[%s] 장-단: %,.2f(%.2f%%), %,.2f, MA_%d: %,.2f, MA_%d: %,.2f, TD: %,d", tradeResult.getCode(), maShort - maLong, MathUtil.getYield(maShort, maLong) * 100, tradeResult.getTradePrice(), properties.getShortPeriod(), maShort, properties.getLongPeriod(), maLong, tradeResult.getTimestampDiff());
        log.debug(message);
    }


    /**
     * @param candleList 캔들
     * @return 이동 평균에서 직전 매수 조건 이면 true, 아니면 false
     */
    private boolean isBeforeBuy(List<Candle> candleList) {
        // 한단계전에 매수 조건이였는지 확인
        List<Candle> beforeCandleList = candleList.subList(1, candleList.size());
        double maShortBefore = CommonTradeHelper.getMa(beforeCandleList, properties.getShortPeriod());
        double maLongBefore = CommonTradeHelper.getMa(beforeCandleList, properties.getLongPeriod());
        double buyTargetPrice = maLongBefore + maLongBefore * properties.getUpBuyRate();
        //(장기이평 + 장기이평 * 상승매수률) <= 단기이평
        return buyTargetPrice <= maShortBefore;
    }
}
