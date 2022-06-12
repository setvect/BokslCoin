package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis.mock;

import com.setvect.bokslcoin.autotrading.slack.SlackMessageService;

public class MockSlackMessageService extends SlackMessageService {
    @Override
    public void sendMessage(String message) {
        // 아무 일도 하지 않음
    }
}
