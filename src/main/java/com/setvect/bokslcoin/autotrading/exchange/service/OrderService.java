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
    private static final String URL_ORDERS_HISTORY = "/v1/orders";
    private static final String URL_ORDERS = "/v1/orders";

    private final AccessTokenMaker accessInfo;

    private final ConnectionInfo connectionInfo;

    /**
     * @param market Market ID<br>
     *               예) KRW-BTC, KRW-ETH, BTC-DOGE
     * @return 주문 가능 정보
     */
    public OrderChance callChange(String market) {
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
    public List<OrderHistory> callHistory(int page, int limit) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("limit", String.valueOf(limit));

        String jsonResult = ApiCaller.requestApi(URL_ORDERS_HISTORY, params, connectionInfo, accessInfo);

        List<OrderHistory> orderHistoryList = GsonUtil.GSON.fromJson(jsonResult, new TypeToken<List<OrderHistory>>() {
        }.getType());

        return orderHistoryList;
    }


    /**
     * 주문하기
     *
     * @param market  마켓 ID<br>
     *                예) KRW-BTC, KRW-ETH, BTC-DOGE
     * @param side    주문 종류
     * @param volume  주문량
     * @param price   주문 가격
     * @param ordType 주문 타입
     * @return 주문 정보
     */
    public OrderResult callOrder(String market, OrderResult.Side side, String volume, String price, OrderResult.OrdType ordType) {
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
