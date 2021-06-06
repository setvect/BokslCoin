package com.setvect.bokslcoin.autotrading.quotation.service;

import com.google.gson.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtils;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandleApiService {
    private static final String URL_MINUTES = "/v1/candles/minutes/{unit}";
    private static final String URL_DAYS = "/v1/candles/days";

    private final ConnectionInfo connectionInfo;


    /**
     * 분(Minute) 캔들
     *
     * @param unit   분 단위. 가능한 값 : 1, 3, 5, 15, 10, 30, 60, 240
     * @param market 마켓 코드 (ex. KRW-BTC)
     * @param to     마지막 캔들 시각 (exclusive).
     * @param count  캔들 개수(최대 200개까지 요청 가능)
     * @return
     */
    public List<CandleMinute> callMinute(int unit, String market, LocalDateTime to, int count) {
        try {
            String url = URL_MINUTES.replace("{unit}", String.valueOf(unit));
            Map<String, String> params = new HashMap<>();

            params.put("market", market);
//            params.put("to", to.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            params.put("count", String.valueOf(count));

            HttpEntity entity = callGetApi(url, params);
            String jsonResult = EntityUtils.toString(entity, "UTF-8");
            List<CandleMinute> accounts = ApplicationUtils.GSON.fromJson(jsonResult, new TypeToken<List<CandleMinute>>() {
            }.getType());

            return accounts;
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
