package com.setvect.bokslcoin.autotrading;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AccessInfo {
    private String accessKey;

    private String secretKey;

    public AccessInfo(
            @Value("${com.setvect.bokslcoin.autotrading.accessKey}") String accessKey,
            @Value("${com.setvect.bokslcoin.autotrading.secretKey}") String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }
}
