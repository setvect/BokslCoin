package com.setvect.bokslcoin.autotrading.common.service;

import com.google.gson.Gson;
import com.setvect.bokslcoin.autotrading.AccessTokenMaker;
import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 업비트 API 호출
 */
@Slf4j
public class ApiCaller {
    /**
     * GET 방식 API 호출
     *
     * @param apiUrl         호출 API 주소(도메인 생략)
     * @param params         파라미터
     * @param connectionInfo 호출 도메인 접속 정보
     * @return 응답 결과(JSON)
     */
    public static String requestApi(String apiUrl, Map<String, String> params, ConnectionInfo connectionInfo) {
        return requestApi(apiUrl, params, connectionInfo, null);
    }

    /**
     * GET 방식 API 호출
     *
     * @param apiUrl           호출 API 주소(도메인 생략)
     * @param params           파라미터
     * @param connectionInfo   호출 도메인 접속 정보
     * @param accessTokenMaker 접근 Token 생성자
     * @return 응답 결과(JSON)
     */
    @SneakyThrows
    public static String requestApi(String apiUrl, Map<String, String> params, ConnectionInfo connectionInfo, AccessTokenMaker accessTokenMaker) {
        String queryString = getQueryString(params);

        String url = connectionInfo.getBaseUrl() + apiUrl + "?" + queryString;
        HttpGet request = new HttpGet(url);
        request.setHeader("Content-Type", "application/json");
        if (accessTokenMaker != null) {
            request.addHeader("Authorization", accessTokenMaker.makeToken(queryString));
        }

        return request(url, request);
    }

    @SneakyThrows
    public static String requestApiByDelete(String apiUrl, Map<String, String> params, ConnectionInfo connectionInfo, AccessTokenMaker accessTokenMaker) {
        String queryString = getQueryString(params);

        String url = connectionInfo.getBaseUrl() + apiUrl + "?" + queryString;
        HttpDelete request = new HttpDelete(url);
        request.setHeader("Content-Type", "application/json");
        request.addHeader("Authorization", accessTokenMaker.makeToken(queryString));

        return request(url, request);
    }

    /**
     * GET 방식  API 호출
     *
     * @param apiUrl           호출 API 주소(도메인 생략)
     * @param params           파라미터
     * @param connectionInfo   호출 도메인 접속 정보
     * @param accessTokenMaker 접근 Token 생성자
     * @return 응답 결과(JSON)
     */
    @SneakyThrows
    public static String requestApiByPost(String apiUrl, Map<String, String> params, ConnectionInfo connectionInfo, AccessTokenMaker accessTokenMaker) {
        String queryString = getQueryString(params);

        String url = connectionInfo.getBaseUrl() + apiUrl;
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.addHeader("Authorization", accessTokenMaker.makeToken(queryString));
        // null 값 제거
        Map<String, String> map = params.entrySet().stream().filter(p -> p.getValue() != null).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
        String requestBody = new Gson().toJson(map);
        request.setEntity(new StringEntity(requestBody));

        return request(url, request);
    }

    private static String request(String url, HttpRequestBase request) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        String jsonText = EntityUtils.toString(entity, "UTF-8");

        if (statusCode != 200 && statusCode != 201) {
            String message = String.format("Error, Status: %d, URL: %s, Message: %s", statusCode, url, jsonText);
            throw new RuntimeException(message);
        }
        return jsonText;
    }

    private static String getQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(p -> p.getValue() != null)
                .map(p -> p.getKey() + "=" + p.getValue())
                .collect(Collectors.joining("&"));
    }

}
