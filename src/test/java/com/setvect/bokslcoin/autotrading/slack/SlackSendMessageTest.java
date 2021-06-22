package com.setvect.bokslcoin.autotrading.slack;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class SlackSendMessageTest {
    @Autowired
    private SlackMessageService slackMessageService;

    @Test
    public void sendMessage() {
        slackMessageService.sendMessage("안녕하세요.");
        System.out.println("끝.");
    }
}
