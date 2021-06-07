package com.setvect.bokslcoin.autotrading;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ConnectionInfo {
    private String baseUrl;

    public ConnectionInfo(@Value("${com.setvect.bokslcoin.autotrading.api.url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
