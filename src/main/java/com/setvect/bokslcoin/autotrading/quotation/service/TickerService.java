package com.setvect.bokslcoin.autotrading.quotation.service;

import com.google.gson.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import com.setvect.bokslcoin.autotrading.common.service.ApiCaller;
import com.setvect.bokslcoin.autotrading.model.Ticker;
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
public class TickerService {
    private static final String URL = "/v1/ticker";

    private final ConnectionInfo connectionInfo;

    /**
     * @param market 마켓 코드 (ex. KRW-BTC)
     * @return 현재가 정보
     */
    public Ticker callTicker(String market) {
        Map<String, String> params = new HashMap<>();
        params.put("markets", market);

        String jsonResult = ApiCaller.requestApi(URL, params, connectionInfo);
        List<Ticker> tickers = GsonUtil.GSON.fromJson(jsonResult, new TypeToken<List<Ticker>>() {
        }.getType());

        return tickers.size() >= 1 ? tickers.get(0) : null;
    }
}
