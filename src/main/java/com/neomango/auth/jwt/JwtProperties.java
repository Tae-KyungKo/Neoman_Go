package com.neomango.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank
        @Size(min = 32)
        String secret,
        @Positive
        long accessTokenValidityInSeconds,
        @Positive
        long refreshTokenValidityInSeconds
) {
}
