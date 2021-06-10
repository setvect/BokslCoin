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
    public Optional<Account> getAccount(String market) {
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

        return find;
    }

    /**
     * @param market 예) KRW, KRW-BTC
     * @return 매수 평균 가격, 해당 코인이 없으면 null
     */
    public Optional<BigDecimal> getAvgBuyPrice(String market) {
        Optional<Account> account = getAccount(market);
        if (account.isPresent()) {
            Double val = Double.valueOf(account.get().getAvgBuyPrice());
            BigDecimal value = BigDecimal.valueOf(val);
            return Optional.of(value);
        }
        return Optional.empty();
    }

    /**
     * @param market 예) KRW, KRW-BTC
     * @return 주문가능 금액/수량
     */
    public BigDecimal getBalance(String market) {
        Optional<Account> account = getAccount(market);
        return account.isPresent() ? BigDecimal.valueOf(Double.valueOf(account.get().getBalance())) : new BigDecimal(0.0);
    }
}
