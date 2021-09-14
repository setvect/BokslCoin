package com.setvect.bokslcoin.autotrading.common;

import com.google.gson.Gson;
import com.setvect.bokslcoin.autotrading.AccessTokenMaker;
import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.util.Map;
import java.util.TreeMap;
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

        return ApplicationUtil.request(url, request);
    }

    @SneakyThrows
    public static String requestApiByDelete(String apiUrl, Map<String, String> params, ConnectionInfo connectionInfo, AccessTokenMaker accessTokenMaker) {
        String queryString = getQueryString(params);

        String url = connectionInfo.getBaseUrl() + apiUrl + "?" + queryString;
        HttpDelete request = new HttpDelete(url);
        request.setHeader("Content-Type", "application/json");
        request.addHeader("Authorization", accessTokenMaker.makeToken(queryString));

        return ApplicationUtil.request(url, request);
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
        // 파라미터 이름 순으로 정렬
        TreeMap<String, String> orderParam = new TreeMap<>(params);
        String queryString = getQueryString(orderParam);

        String url = connectionInfo.getBaseUrl() + apiUrl;
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.addHeader("Authorization", accessTokenMaker.makeToken(queryString));
        // null 값 제거, 파라미터 이름 순으로 정렬, querystring과 순서가 같아야 jwt 인증 오류 발생하지 않음
        TreeMap<String, String> map = orderParam.entrySet().stream()
                .filter(p -> p.getValue() != null)
                .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue(), (p1, p2) -> p1, TreeMap::new));
        String requestBody = new Gson().toJson(map);
        request.setEntity(new StringEntity(requestBody));

        return ApplicationUtil.request(url, request);
    }


    private static String getQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(p -> p.getValue() != null)
                .map(p -> p.getKey() + "=" + p.getValue())
                .collect(Collectors.joining("&"));
    }

}
