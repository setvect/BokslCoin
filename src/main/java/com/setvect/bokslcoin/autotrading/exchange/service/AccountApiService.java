package com.setvect.bokslcoin.autotrading.exchange.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.AccessInfo;
import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtils;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountApiService {
    private static final String URL = "/v1/accounts";

    private final AccessInfo accessInfo;

    private final ConnectionInfo connectionInfo;

    public List<Account> call() {
        try {
            HttpEntity entity = callGetApi(URL);
            String jsonResult = EntityUtils.toString(entity, "UTF-8");
            List<Account> accounts = GsonUtil.GSON.fromJson(jsonResult, new TypeToken<List<Account>>() {
            }.getType());

            return accounts;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private HttpEntity callGetApi(String apiUrl) throws IOException {
        String authenticationToken = getAuthToken();
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(connectionInfo.getBaseUrl() + apiUrl);
        request.setHeader("Content-Type", "application/json");
        request.addHeader("Authorization", authenticationToken);

        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        return entity;
    }

    private String getAuthToken() {
        Algorithm algorithm = Algorithm.HMAC256(accessInfo.getSecretKey());
        String jwtToken = JWT.create()
                .withClaim("access_key", accessInfo.getAccessKey())
                .withClaim("nonce", UUID.randomUUID().toString())
                .sign(algorithm);

        String authenticationToken = "Bearer " + jwtToken;
        return authenticationToken;
    }

}
