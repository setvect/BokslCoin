package com.setvect.bokslcoin.autotrading.schedule.service;

import com.setvect.bokslcoin.autotrading.exchange.service.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.service.OrderService;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.quotation.service.CandleService;
import com.setvect.bokslcoin.autotrading.quotation.service.TickerService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingScheduleService {
    private final AccountService accountService;
    private final CandleService candleService;
    private final TickerService tickerService;
    private final OrderService orderService;

    @Value("${com.setvect.bokslcoin.autotrading.coin}")
    private String marketCoin;

    @Scheduled(fixedDelay = 5000, initialDelay = 1000)
    public void scheduleCheck() {
        log.info("call scheduleCheck()");
        List<Account> account = accountService.getMyAccount();
        System.out.println(account);

        Optional<Account> coin = account.stream().filter(p -> !p.getCurrency().equals("KRW")).findAny();
        coin.ifPresent(p -> {
            System.out.println(p);
            double price = Double.valueOf(p.getAvgBuyPrice()) * 1.01;
            String askPrice = ApplicationUtils.applyAskPrice(price, 1000);
            orderService.callOrderAsk("KRW-" + p.getCurrency(), p.getBalance(), askPrice);
//            orderService.callOrderAskByMarket(marketCoin, p.getBalance());
//            orderService.callOrderBidByMarket(marketCoin, "5000");
        });


//        System.out.println("=====================");
//        List<CandleMinute> candleList = candleService.callMinute(1, "KRW-BTC", LocalDateTime.now(), 10);
//        System.out.println(candleList);
//        System.out.println("=====================");
//        Ticker ticker = tickerService.callTicker("KRW-BTC");
//        System.out.println(ticker);
//        System.out.println("=====================");
//        OrderChance orderChance = orderService.getChange("KRW-BTC");
//        System.out.println(orderChance);
//
//        List<OrderHistory> orderHistoryList = orderService.getHistory(1, 100);
//        System.out.println(orderHistoryList);

//        if (!orderHistoryList.isEmpty()) {
//            OrderHistory orderHistory = orderService.cancelOrder(orderHistoryList.get(0).getUuid());
//            System.out.println(orderHistory);
//        }


        System.out.println();
        System.out.println();
        System.out.println();
    }

}
