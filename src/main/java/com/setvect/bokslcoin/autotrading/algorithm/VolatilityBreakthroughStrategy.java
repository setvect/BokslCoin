package com.setvect.bokslcoin.autotrading.algorithm;

import com.setvect.bokslcoin.autotrading.exchange.service.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.service.OrderService;
import com.setvect.bokslcoin.autotrading.model.Account;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.quotation.service.CandleService;
import com.setvect.bokslcoin.autotrading.quotation.service.TickerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 변동성 돌파 전략
 */
@Service("vbs")
@Slf4j
@RequiredArgsConstructor
public class VolatilityBreakthroughStrategy implements CoinTrading {
    private final AccountService accountService;
    private final CandleService candleService;
    private final TickerService tickerService;
    private final OrderService orderService;

    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbs.coin}")
    private String coin;

    @Override
    public void apply() {
        log.info("call VolatilityBreakthroughStrategy");

        BigDecimal krw = accountService.getBalance("KRW");
        BigDecimal btc = accountService.getBalance(coin);

        List<CandleDay> candleList = candleService.getDay("KRW-BTC", 2);
        CandleDay yesterday = candleList.get(1);
        System.out.println(yesterday);

//        Optional<Account> coin = account.stream().filter(p -> !p.getCurrency().equals("KRW")).findAny();
//        coin.ifPresent(p -> {
//            System.out.println(p);
//            double price = Double.valueOf(p.getAvgBuyPrice()) * 1.01;
//            String askPrice = ApplicationUtils.applyAskPrice(price, 1000);
//            orderService.callOrderAsk("KRW-" + p.getCurrency(), p.getBalance(), askPrice);
////            orderService.callOrderAskByMarket(marketCoin, p.getBalance());
////            orderService.callOrderBidByMarket(marketCoin, "5000");
//        });


//        System.out.println("=====================");
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

    }
}
