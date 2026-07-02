package com.neomango.auth.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.neomango.auth.jwt.JwtProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";

	private final RedisTemplate<String, String> redisTemplate;
	private final JwtProperties jwtProperties;

	public void save(Long userId, String refreshToken) {
		validateUserId(userId);
		validateRefreshToken(refreshToken);

		long refreshTokenValidityInSeconds = jwtProperties.refreshTokenValidityInSeconds();
		if (refreshTokenValidityInSeconds <= 0) {
			throw new IllegalStateException("Refresh token validity must be positive.");
		}

		redisTemplate.opsForValue()
			.set(refreshTokenKey(userId), refreshToken, Duration.ofSeconds(refreshTokenValidityInSeconds));
	}

	public Optional<String> findByUserId(Long userId) {
		validateUserId(userId);

		return Optional.ofNullable(redisTemplate.opsForValue().get(refreshTokenKey(userId)));
	}

	public void delete(Long userId) {
		validateUserId(userId);

		redisTemplate.delete(refreshTokenKey(userId));
	}

	public boolean matches(Long userId, String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			return false;
		}

		return findByUserId(userId)
			.map(savedRefreshToken -> savedRefreshToken.equals(refreshToken))
			.orElse(false);
	}

	private String refreshTokenKey(Long userId) {
		validateUserId(userId);

		return REFRESH_TOKEN_KEY_PREFIX + userId;
	}

	private void validateUserId(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("userId must not be null.");
		}
	}

	private void validateRefreshToken(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new IllegalArgumentException("refreshToken must not be blank.");
		}
	}
}
