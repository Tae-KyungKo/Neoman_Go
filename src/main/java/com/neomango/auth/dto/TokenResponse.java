package com.neomango.auth.dto;

public record TokenResponse(
	String accessToken,
	String refreshToken,
	String tokenType,
	long accessTokenExpiresIn
) {

	public TokenResponse(String accessToken, String refreshToken, String tokenType) {
		this(accessToken, refreshToken, tokenType, 0L);
	}
}

