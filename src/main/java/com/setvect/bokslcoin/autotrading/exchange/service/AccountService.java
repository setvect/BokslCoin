package com.setvect.bokslcoin.autotrading.exchange.service;

import com.google.gson.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.AccessTokenMaker;
import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import com.setvect.bokslcoin.autotrading.common.service.ApiCaller;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    private static final String URL = "/v1/accounts";

    private final AccessTokenMaker accessInfo;

    private final ConnectionInfo connectionInfo;

    public List<Account> call() {
        String jsonResult = ApiCaller.requestApi(URL, Collections.emptyMap(), connectionInfo, accessInfo);
        List<Account> accounts = GsonUtil.GSON.fromJson(jsonResult, new TypeToken<List<Account>>() {
        }.getType());

        return accounts;
    }
}
