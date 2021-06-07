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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    private static final String URL = "/v1/accounts";

    private final AccessTokenMaker accessInfo;

    private final ConnectionInfo connectionInfo;

    /**
     * @return 계좌 내역
     */
    public List<Account> getMyAccount() {
        String jsonResult = ApiCaller.requestApi(URL, Collections.emptyMap(), connectionInfo, accessInfo);
        List<Account> accounts = GsonUtil.GSON.fromJson(jsonResult, new TypeToken<List<Account>>() {
        }.getType());

        return accounts;
    }


    /**
     * @param market 예) KRW, KRW-BTC
     * @return 내 계좌 정보
     */
    public BigDecimal getBalance(String market) {
        String[] tokens = market.split("-");
        final String currency;
        final String unitCurrency;
        if (tokens.length == 1) {
            currency = tokens[0];
            unitCurrency = null;
        } else {
            currency = tokens[1];
            unitCurrency = tokens[0];
        }

        List<Account> account = getMyAccount();

        Optional<Account> find = account.stream()
                .filter(p -> p.getCurrency().equals(currency))
                .filter(p -> unitCurrency == null || p.getUnitCurrency().equals(unitCurrency))
                .findAny();

        return find.isPresent() ? new BigDecimal(Double.valueOf(find.get().getBalance())) : new BigDecimal(0.0);
    }

}
