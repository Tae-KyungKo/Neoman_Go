package com.neomango.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenValidityInSeconds,
        long refreshTokenValidityInSeconds
) {
}