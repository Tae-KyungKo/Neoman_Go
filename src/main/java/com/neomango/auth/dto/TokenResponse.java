package com.neomango.auth.dto;

public record TokenResponse(
	String accessToken,
	String refreshToken,
	String tokenType
) {
}

