package com.setvect.bokslcoin.autotrading.schedule.service;

import com.setvect.bokslcoin.autotrading.exchange.service.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.service.OrderService;
import com.setvect.bokslcoin.autotrading.model.OrderHistory;
import com.setvect.bokslcoin.autotrading.quotation.service.CandleService;
import com.setvect.bokslcoin.autotrading.quotation.service.TickerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingScheduleService {
    private final AccountService accountService;
    private final CandleService candleService;
    private final TickerService tickerService;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 5000, initialDelay = 1000)
    public void scheduleCheck() {
        log.info("call scheduleCheck()");
//        List<Account> account = accountService.call();
//        System.out.println(account);
//        System.out.println("=====================");
//        List<CandleMinute> candleList = candleService.callMinute(1, "KRW-BTC", LocalDateTime.now(), 10);
//        System.out.println(candleList);
//        System.out.println("=====================");
//        Ticker ticker = tickerService.callTicker("KRW-BTC");
//        System.out.println(ticker);
//        System.out.println("=====================");
//        OrderChance orderChance = orderService.callChange("KRW-BTC");
//        System.out.println(orderChance);

        List<OrderHistory> orderHistoryList = orderService.getHistory(1, 100);
        System.out.println(orderHistoryList);

        if (!orderHistoryList.isEmpty()) {
            OrderHistory orderHistory = orderService.cancelOrder(orderHistoryList.get(0).getUuid());
            System.out.println(orderHistory);
        }

        System.out.println();
        System.out.println();
        System.out.println();
    }
}
