package com.neomango.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.neomango.auth.dto.TokenResponse;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

	private static final String ROLE_CLAIM = "role";
	private static final String TOKEN_TYPE_CLAIM = "tokenType";
	private static final String ACCESS_TOKEN_TYPE = "ACCESS";
	private static final String REFRESH_TOKEN_TYPE = "REFRESH";
	private static final String BEARER_TOKEN_TYPE = "Bearer";

	private final JwtProperties jwtProperties;
	private final SecretKey secretKey;

	public JwtTokenProvider(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
		this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public TokenResponse createToken(User user) {
		return createToken(user.getId(), user.getRole());
	}

	public TokenResponse createToken(Long userId, UserRole role) {
		return new TokenResponse(
			createAccessToken(userId, role),
			createRefreshToken(userId),
			BEARER_TOKEN_TYPE
		);
	}

	public String createAccessToken(Long userId, UserRole role) {
		Instant now = Instant.now();
		Instant expiration = now.plusSeconds(jwtProperties.accessTokenValidityInSeconds());

		return Jwts.builder()
			.subject(String.valueOf(userId))
			.claim(ROLE_CLAIM, role.name())
			.claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiration))
			.signWith(secretKey)
			.compact();
	}

	public String createRefreshToken(Long userId) {
		Instant now = Instant.now();
		Instant expiration = now.plusSeconds(jwtProperties.refreshTokenValidityInSeconds());

		return Jwts.builder()
			.subject(String.valueOf(userId))
			.claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiration))
			.signWith(secretKey)
			.compact();
	}

	public boolean validateToken(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	public boolean isAccessToken(String token) {
		return ACCESS_TOKEN_TYPE.equals(getTokenType(token));
	}

	public boolean isRefreshToken(String token) {
		return REFRESH_TOKEN_TYPE.equals(getTokenType(token));
	}

	public Long getUserId(String token) {
		return Long.valueOf(parseClaims(token).getSubject());
	}

	public UserRole getRole(String token) {
		return UserRole.valueOf(parseClaims(token).get(ROLE_CLAIM, String.class));
	}

	public String getTokenType(String token) {
		return parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}
}

