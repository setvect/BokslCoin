package com.setvect.bokslcoin.autotrading.api;

import com.setvect.bokslcoin.autotrading.AccessTokenMaker;
import com.setvect.bokslcoin.autotrading.exchange.OrderService;
import com.setvect.bokslcoin.autotrading.model.OrderHistory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
class OrderServiceTest {
    @Autowired
    private OrderService orderService;

    @Autowired
    private AccessTokenMaker accessTokenMaker;

    @Test
    void orderTest() {
        List<OrderHistory> history = orderService.getHistory(0, 100);
        for (OrderHistory orderHistory : history) {
            System.out.println(orderHistory);
        }
        System.out.println("끝.");
    }

    @Test
    void makeToken() {
        for (int i = 0; i < 10; i++) {
            String s = accessTokenMaker.makeToken("limit=10&page=0&state=wait");
            System.out.println(s);
        }
    }
}
