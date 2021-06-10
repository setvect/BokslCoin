package com.setvect.bokslcoin.autotrading.algorithm;

import com.setvect.bokslcoin.autotrading.exchange.service.AccountService;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import com.setvect.bokslcoin.autotrading.util.DateRange;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 변동성 돌파 전략 + 손절, 익절 알고리즘
 */
@Slf4j
@RequiredArgsConstructor
public class VbsStopService {
    /*
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
    /**
     * 매매 코인 예) KRW-BTC, KRW-ETH
     */
    private final String market;
    /**
     * 손절률 0.05이면 매수가 대비 -5%이하로 가격이 떨어지면 매도
     */
    private final double loseStop;
    /**
     * 익절률 0.10이면 매수가 대비 10%이상으로 가격이 오르면 매도
     */
    private final double gainStop;

    private final TradeService tradeService;

    private final AccountService accountService;

    /**
     * 총 현금을 기준으로 투자 비율
     * 1은 100%, 0.5은 50% 투자
     */
    private final double investRatio;


    /**
     * 변동성 돌파 판단 비율
     */
    private final double k;

    /**
     * 매수 접수 시간 범위
     */
    private DateRange bidRange;
    /**
     * 매도 접수 시간 범위
     */
    private DateRange askRange;
    /**
     * 시장가 매수 요청 시점 거래 가격
     */
    private double bidPrice;

    /**
     * 시장가매도 요청 시점 거래 가격
     */
    private double askPrice;

    /**
     * 매도 여부
     */
    private boolean asked;

    private int currentDay = 0;

    /**
     * 매도 유형
     */
    public enum AskType {
        /**
         * 매도 시간 경과
         */
        TIME,
        /**
         * 손절 매도
         */
        LOSS,
        /**
         * 익절 매도
         */
        GAIN
    }

    @Builder
    public VbsStopService(TradeService tradeService, AccountService accountService, String market, double k, double investRatio, double loseStop, double gainStop) {
        this.market = market;
        this.loseStop = loseStop;
        this.gainStop = gainStop;
        this.tradeService = tradeService;
        this.accountService = accountService;
        this.investRatio = investRatio;
        this.k = k;
    }

    /**
     * @param candleMinute 분봉 데이터
     * @param yesterday    어제 일봉 데이터
     */
    public void process(CandleMinute candleMinute, CandleDay yesterday) {
        LocalDateTime candleDateTimeUtc = candleMinute.getCandleDateTimeUtc();
        LocalDateTime candleDateTimeKst = candleMinute.getCandleDateTimeKst();
        if (currentDay != candleDateTimeUtc.getDayOfMonth()) {
            // 날짜가 변경되면 매도 초기화
            asked = false;
            log.info(String.format("change day. CurrentDay(UTC): %s \n", DateUtil.formatDateTime(candleDateTimeUtc)));
        }

        log.debug(String.format("현재 시간: %s, 매수 시간: %s, 매도 시간: %s, %s: %,f", DateUtil.formatDateTime(candleMinute.getCandleDateTimeKst()), bidRange, askRange, market, candleMinute.getTradePrice()));
        Optional<BigDecimal> avgByPrice = accountService.getAvgBuyPrice(market);

        // 매수를 했을 경우 매도 타이밍 체크
        if (avgByPrice.isPresent()) {
            // 매도 시간이면 무조건 매도

            if (askRange.isBetween(candleDateTimeKst)) {
                doAsk(AskType.TIME);
            }

            double avgPrice = avgByPrice.get().doubleValue();
            double rate = candleMinute.getTradePrice() / avgPrice - 1;
            // 이익인 경우
            if (rate > 0) {
                if (this.gainStop <= rate) {
                    doAsk(AskType.GAIN);
                }
            }
            // 손실인 경우
            else {
                if (this.loseStop <= -rate) {
                    doAsk(AskType.LOSS);
                }
            }
            return;
        }

        // 매도 했을 경우 당일 날은 아무것도 안한다.
        if (asked) {
            return;
        }

        if (bidRange.isBetween(candleDateTimeKst)) {
            double targetValue = getTargetPrice(yesterday);
            log.debug(String.format("%s 목표가: %,f\t현재가: %,f",market, targetValue, candleMinute.getTradePrice()));

            if (targetValue > candleMinute.getTradePrice()) {
                log.info("목표가 도달하지 않음");
                return;
            }

            BigDecimal krw = accountService.getBalance("KRW");
            // 매수 금액
            double askPrice = krw.doubleValue() * investRatio;
            log.info(String.format("★★★ 시장가 매수, 코인: %s, 현재가: %,f, 매수 금액: %,f,", market, investRatio, askPrice));
            doBid(askPrice);
        }

    }

    /**
     * 매수
     * @param askPrice
     */
    private void doBid(double askPrice) {
        tradeService.bid(askPrice);
    }

    private void doAsk(AskType askType) {
        tradeService.ask(askType);
        asked = true;
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
    private double getTargetPrice(CandleDay yesterday) {
        double targetValue = yesterday.getTradePrice() + (yesterday.getHighPrice() - yesterday.getLowPrice()) * k;
        log.debug(String.format("목표가: %,.2f = 종가: %,.2f + (고가: %,.2f - 저가: %,.2f) * K값: %,.2f"
                , targetValue, yesterday.getTradePrice(), yesterday.getHighPrice(), yesterday.getLowPrice(), k));
        return targetValue;
    }
}
