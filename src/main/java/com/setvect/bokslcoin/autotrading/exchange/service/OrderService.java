package com.setvect.bokslcoin.autotrading.exchange.service;

import com.google.gson.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.AccessTokenMaker;
import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import com.setvect.bokslcoin.autotrading.common.service.ApiCaller;
import com.setvect.bokslcoin.autotrading.model.OrderChance;
import com.setvect.bokslcoin.autotrading.model.OrderHistory;
import com.setvect.bokslcoin.autotrading.model.OrderResult;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private static final String URL_ORDERS_CHANCE = "/v1/orders/chance";
    private static final String URL_ORDERS = "/v1/orders";
    private static final String URL_ORDER = "/v1/order";

    private final AccessTokenMaker accessInfo;

    private final ConnectionInfo connectionInfo;

    /**
     * @param market Market ID<br>
     *               예) KRW-BTC, KRW-ETH, BTC-DOGE
     * @return 주문 가능 정보
     */
    public OrderChance getChange(String market) {
        Map<String, String> params = new HashMap<>();
        params.put("market", market);

        String jsonResult = ApiCaller.requestApi(URL_ORDERS_CHANCE, params, connectionInfo, accessInfo);
        OrderChance orderChange = GsonUtil.GSON.fromJson(jsonResult, OrderChance.class);

        return orderChange;
    }

    /**
     * @param page  페이지 1부터 시작
     * @param limit 페이지당 가져올 항목 수 100 이하
     * @return 주문 내역
     */
    public List<OrderHistory> getHistory(int page, int limit) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("limit", String.valueOf(limit));

        String jsonResult = ApiCaller.requestApi(URL_ORDERS, params, connectionInfo, accessInfo);

        List<OrderHistory> orderHistoryList = GsonUtil.GSON.fromJson(jsonResult, new TypeToken<List<OrderHistory>>() {
        }.getType());

        return orderHistoryList;
    }


    /**
     * 주문 취소
     *
     * @param uuid 취소할 주문의 UUID
     * @return 취소 주문 정보
     */
    public OrderHistory cancelOrder(String uuid) {
        Map<String, String> params = new HashMap<>();
        params.put("uuid", String.valueOf(uuid));

        String jsonResult = ApiCaller.requestApiByDelete(URL_ORDER, params, connectionInfo, accessInfo);
        OrderHistory orderHistory = GsonUtil.GSON.fromJson(jsonResult, OrderHistory.class);
        return orderHistory;
    }


    /**
     * 지정가 매수 주문하기
     *
     * @param market 마켓 ID<br>
     *               예) KRW-BTC, KRW-ETH, BTC-DOGE
     * @param volume 주문량
     * @param price  주문 가격
     * @return 주문 정보
     */
    public OrderResult callOrderBid(String market, String volume, String price) {
        return callOrder(market, volume, price, OrderResult.OrdType.limit, OrderResult.Side.bid);
    }

    /**
     * 지정가 매도 주문하기
     *
     * @param market 마켓 ID<br>
     *               예) KRW-BTC, KRW-ETH, BTC-DOGE
     * @param volume 주문량
     * @param price  주문 가격
     * @return 주문 정보
     */
    public OrderResult callOrderAsk(String market, String volume, String price) {
        return callOrder(market, volume, price, OrderResult.OrdType.limit, OrderResult.Side.ask);
    }

    /**
     * 시장가 매수 주문하기
     *
     * @param market 마켓 ID<br>
     *               예) KRW-BTC, KRW-ETH, BTC-DOGE
     * @param volume 주문량
     * @return 주문 정보
     */
    public OrderResult callOrderBidPrice(String market, String volume) {
        return callOrder(market, volume, null, OrderResult.OrdType.price, OrderResult.Side.bid);
    }

    /**
     * 시장가 매도 주문하기
     *
     * @param market 마켓 ID<br>
     *               예) KRW-BTC, KRW-ETH, BTC-DOGE
     * @param volume 주문량
     * @return 주문 정보
     */
    public OrderResult callOrderAskMarket(String market, String volume) {
        return callOrder(market, volume, null, OrderResult.OrdType.market, OrderResult.Side.ask);
    }

    /**
     * 주문하기
     *
     * @param market  마켓 ID<br>
     *                예) KRW-BTC, KRW-ETH, BTC-DOGE
     * @param volume  주문량
     * @param price   주문 가격
     * @param ordType 주문 타입
     * @param side    주문 종류(매수, 매도)
     * @return 주문 정보
     */
    private OrderResult callOrder(String market, String volume, String price, OrderResult.OrdType ordType, OrderResult.Side side) {
        Map<String, String> params = new HashMap<>();
        params.put("market", market);
        params.put("side", side.name());
        params.put("volume", volume);
        params.put("price", price);
        params.put("ord_type", ordType.name());

        String jsonResult = ApiCaller.requestApiByPost(URL_ORDERS, params, connectionInfo, accessInfo);
        OrderResult order = GsonUtil.GSON.fromJson(jsonResult, OrderResult.class);
        return order;
    }
}
