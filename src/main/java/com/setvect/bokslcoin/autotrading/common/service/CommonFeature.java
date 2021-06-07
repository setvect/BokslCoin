package com.setvect.bokslcoin.autotrading.common.service;

import com.setvect.bokslcoin.autotrading.AccessTokenMaker;
import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class CommonFeature {

    public static String requestApi(String apiUrl, Map<String, String> params, ConnectionInfo connectionInfo) throws URISyntaxException, IOException {
        return requestApi(apiUrl, params, connectionInfo, null);
    }

    @SneakyThrows
    public static String requestApi(String apiUrl, Map<String, String> params, ConnectionInfo connectionInfo, AccessTokenMaker accessInfo) {
        String queryString = params.entrySet().stream()
                .map(p -> p.getKey() + "=" + p.getValue())
                .collect(Collectors.joining("&"));

        HttpClient client = HttpClientBuilder.create().build();
        String url = connectionInfo.getBaseUrl() + apiUrl + "?" + queryString;
        HttpGet request = new HttpGet(url);
        request.setHeader("Content-Type", "application/json");
        if (accessInfo != null) {
            request.addHeader("Authorization", accessInfo.makeToken(queryString));
        }
        HttpResponse response = client.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        String jsonText = EntityUtils.toString(entity, "UTF-8");

        if (statusCode != 200) {
            String message = String.format("Error, Status: %d, URL: %s, Message: %s", statusCode, url, jsonText);
            throw new RuntimeException(message);
        }
        return jsonText;
    }
}
