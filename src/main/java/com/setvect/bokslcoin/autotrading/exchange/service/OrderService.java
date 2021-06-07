package com.setvect.bokslcoin.autotrading.exchange.service;

import com.setvect.bokslcoin.autotrading.AccessTokenMaker;
import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import com.setvect.bokslcoin.autotrading.common.service.CommonFeature;
import com.setvect.bokslcoin.autotrading.model.OrderChance;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private static final String URL = "/v1/orders/chance";

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

        String jsonResult = CommonFeature.requestApi(URL, params, connectionInfo, accessInfo);
        OrderChance orderChange = GsonUtil.GSON.fromJson(jsonResult, OrderChance.class);

        return orderChange;
    }
}
