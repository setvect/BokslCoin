package com.setvect.bokslcoin.autotrading;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.UUID;

@Component
@Getter
public class AccessTokenMaker {
    private String accessKey;

    private String secretKey;

    public AccessTokenMaker(
            @Value("${com.setvect.bokslcoin.autotrading.api.accessKey}") String accessKey,
            @Value("${com.setvect.bokslcoin.autotrading.api.secretKey}") String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @SneakyThrows
    public String makeToken(String queryString) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        String queryHash = null;
        if (StringUtils.isNotEmpty(queryString)) {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(queryString.getBytes("UTF-8"));
            queryHash = String.format("%0128x", new BigInteger(1, md.digest()));
        }
        String jwtToken = JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .withClaim("query_hash", queryHash)
                .withClaim("query_hash_alg", "SHA512")
                .sign(algorithm);

        String authenticationToken = "Bearer " + jwtToken;
        return authenticationToken;
    }
}
