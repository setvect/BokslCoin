package com.setvect.bokslcoin.autotrading.api;

import com.setvect.bokslcoin.autotrading.exchange.AccountService;
import com.setvect.bokslcoin.autotrading.model.Account;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class AccountServiceTest {
    @Autowired
    private AccountService accountService;

    @Test
    public void accountTest() {
        List<Account> myAccount = accountService.getMyAccount();
        for (Account account : myAccount) {
            System.out.println(account);
        }
        System.out.println("끝.");
    }
}
