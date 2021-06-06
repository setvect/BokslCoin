package com.setvect.bokslcoin.autotrading.quotation.service;

import com.google.gson.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import com.setvect.bokslcoin.autotrading.model.Ticker;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TickerApiService {
    private static final String URL = "/v1/ticker";

    private final ConnectionInfo connectionInfo;

    /**
     * 요청 당시 종목의 스냅샷을 반환한다.
     *
     * @param market 마켓 코드 (ex. KRW-BTC)
     * @return
     */
    public List<Ticker> callTicker(String market) {
        try {
            Map<String, String> params = new HashMap<>();

            params.put("markets", market);

            HttpEntity entity = callGetApi(URL, params);
            String jsonResult = EntityUtils.toString(entity, "UTF-8");
            List<Ticker> tickers = GsonUtil.GSON.fromJson(jsonResult, new TypeToken<List<Ticker>>() {
            }.getType());

            return tickers;
        } catch (IOException | URISyntaxException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private HttpEntity callGetApi(String apiUrl, Map<String, String> params) throws IOException, URISyntaxException {
        HttpClient client = HttpClientBuilder.create().build();
        String url = connectionInfo.getBaseUrl() + apiUrl;
        HttpGet request = new HttpGet(url);
        URIBuilder uriBuilder = new URIBuilder(request.getURI());

        params.entrySet().forEach(p -> {
            uriBuilder.addParameter(p.getKey(), p.getValue());
        });

        URI uri = uriBuilder.build();
        request.setHeader("Content-Type", "application/json");
        request.setURI(uri);

        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        return entity;
    }

}
