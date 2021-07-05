package com.setvect.bokslcoin.autotrading.algorithm;

import com.setvect.bokslcoin.autotrading.model.Candle;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 매매시 발생하는 이벤트
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BasicTradeEvent implements TradeEvent {
    private final SlackMessageService slackMessageService;

    @Override
    public void newPeriod(Candle candle) {
        LocalDateTime localDateTime = candle.getCandleDateTimeUtc();
        log.info("새로운 매매주기: {}", DateUtil.formatDateTime(localDateTime));
    }

    @Override
    public void check(Candle candle) {
        // nothing
    }

    @Override
    public void setMaPrice(double ma) {

    }

    @Override
    public void check(CandleMinute candle, double maShort, double maLong) {
    }

    @Override
    public void check(CandleMinute candle, double currentMa, double maxMa, double minMa) {
    }

    @Override
    public void bid(String market, double tradePrice, double bidPrice) {
        String message = String.format("★★★ 시장가 매수, 코인: %s, 현재가: %,.2f, 매수 금액: %,.0f,", market, tradePrice, bidPrice);
        log.info(message);
        slackMessageService.sendMessage(message);
    }

    @Override
    public void ask(String market, double balance, double tradePrice, AskReason reason) {
        String message = String.format("★★★ 시장가 매도, 코인: %s, 보유량: %f, 현재가: %,.2f, 예상 금액: %,.0f, 매도이유: %s", market, balance, tradePrice, balance * tradePrice, reason);
        log.info(message);
        slackMessageService.sendMessage(message);
    }

    @Override
    public void registerTargetPrice(String market, double targetPrice) {
        String message = String.format("코인: %s, 매수 목표가: %,.2f ", market, targetPrice);
        log.info(message);
        slackMessageService.sendMessage(message);
    }
}
