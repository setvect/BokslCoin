package com.setvect.bokslcoin.autotrading.backtest.common.mock;

import com.setvect.bokslcoin.autotrading.exchange.AccountService;
import com.setvect.bokslcoin.autotrading.model.Account;
import lombok.Setter;

import java.util.Map;
import java.util.stream.Collectors;

public class MockAccountService extends AccountService {
    @Setter
    private Map<String, Account> accountMap;

    public MockAccountService() {
        super(new MockAccessTokenMaker(), new MockConnectionInfo());
    }

    /**
     * @return Key: 코인(KRW, KRW-BTC, ...), Value: 가격정보
     */
    public Map<String, Account> getMyAccountBalance() {
        return accountMap.entrySet().stream()
                .filter(e -> e.getValue().getBalanceValue() != 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
