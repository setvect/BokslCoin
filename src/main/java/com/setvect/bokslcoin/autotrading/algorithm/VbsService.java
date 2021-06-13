package com.setvect.bokslcoin.autotrading.algorithm;

import com.setvect.bokslcoin.autotrading.exchange.service.AccountService;
import com.setvect.bokslcoin.autotrading.exchange.service.OrderService;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.Ticker;
import com.setvect.bokslcoin.autotrading.quotation.service.CandleService;
import com.setvect.bokslcoin.autotrading.quotation.service.TickerService;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 변동성 돌파 전략을 사용한 매매 알고리즘
 */
@Service("vbs")
@Slf4j
@RequiredArgsConstructor
public class VbsService implements CoinTrading {
    /**
     * 매수 가능 시작 시간
     */
    private static final String BID_FROM_TIME = "00:01:00";
    /**
     * 매수 가능 종료 시간
     */
    private static final String BID_TO_TIME = "23:59:00";
    /**
     * 매도 가능 시작 시간
     */
    private static final String ASK_FROM_TIME = "23:59:01";
    /**
     * 매도 가능 종료 시간
     */
    private static final String ASK_TO_TIME = "23:59:59";

    private final AccountService accountService;
    private final CandleService candleService;
    private final TickerService tickerService;
    private final OrderService orderService;

    /**
     * 매수, 매도 대상 코인
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbs.coin}")
    private String coin;

    /**
     * 변동성 돌파 판단 비율
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbs.k}")
    private double k;

    /**
     * 총 현금을 기준으로 투자 비율
     * 1은 100%, 0.5은 50% 투자
     */
    @Value("${com.setvect.bokslcoin.autotrading.algorithm.vbs.rate}")
    private double rate;

    /**
     * 매수 접수 시간 범위
     */
    private DateRange bidRange;
    /**
     * 매도 접수 시간 범위
     */
    private DateRange askRange;


    @Override
    public void apply() {
        initTime();
        BigDecimal coinBalance = accountService.getBalance(coin);
        LocalDateTime now = LocalDateTime.now();
        double currentPrice = getCurrentPrice();

        log.debug(String.format("현재 시간: %s, 매수 시간: %s, 매도 시간: %s, %s: %,f", DateUtil.formatDateTime(LocalDateTime.now()), bidRange, askRange, coin, currentPrice));

        //  코인 매수를 했다면
        double balance = coinBalance.doubleValue();
        if (balance > 0.00001) {
            // 매도 시간 파악
            if (askRange.isBetween(now)) {
                log.info(String.format("★★★ 시장가 매도, 코인: %s 보유량: %,f, 현재가: %,f, 예상 금액: %,f", coin, balance, currentPrice, balance * currentPrice));
                orderService.callOrderAskByMarket(coin, ApplicationUtil.toNumberString(balance));
            }
            return;
        }
        if (bidRange.isBetween(now)) {
            double targetValue = getTargetPrice();
            log.debug(String.format("%s 목표가: %,f\t현재가: %,f", coin, targetValue, currentPrice));

            if (targetValue > currentPrice) {
                log.debug("목표가 도달하지 않음");
                return;
            }

            BigDecimal krw = accountService.getBalance("KRW");
            // 매수 금액
            double askPrice = krw.doubleValue() * rate;
            log.info(String.format("★★★ 시장가 매수, 코인: %s, 현재가: %,f, 매수 금액: %,f,", coin, currentPrice, askPrice));
            orderService.callOrderBidByMarket(coin, ApplicationUtil.toNumberString(askPrice));
        }
    }

    /**
     * @return 매매 대상 코인 현재 가격
     */
    private double getCurrentPrice() {
        Ticker ticker = tickerService.getTicker(coin);
        return ticker.getTradePrice();
    }

    /**
     * 매수, 매도 시간 범위 설정
     */
    private void initTime() {
        // 매도 범위
        this.bidRange = ApplicationUtil.getDateRange(BID_FROM_TIME, BID_TO_TIME);
        this.askRange = ApplicationUtil.getDateRange(ASK_FROM_TIME, ASK_TO_TIME);
    }

    /**
     * @return 매수를 하기위한 목표 가격
     */
    private double getTargetPrice() {
        List<CandleDay> candleList = candleService.getDay(coin, 2);
        CandleDay yesterday = candleList.get(1);

        double targetValue = yesterday.getTradePrice() + (yesterday.getHighPrice() - yesterday.getLowPrice()) * k;
        log.debug(String.format("목표가: %,.2f = 종가: %,.2f + (고가: %,.2f - 저가: %,.2f) * K값: %,.2f"
                , targetValue, yesterday.getTradePrice(), yesterday.getHighPrice(), yesterday.getLowPrice(), k));

        return targetValue;
    }
}
