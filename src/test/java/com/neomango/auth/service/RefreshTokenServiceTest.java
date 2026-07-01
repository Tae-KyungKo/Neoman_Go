package com.neomango.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.neomango.auth.jwt.JwtProperties;

@ActiveProfiles("test")
@SpringBootTest
class RefreshTokenServiceTest {

	private static final Long USER_ID = 999001L;
	private static final Long OTHER_USER_ID = 999002L;
	private static final String EMAIL = "tester@example.com";
	private static final String LOGIN_ID = "tester01";
	private static final String REFRESH_TOKEN = "refresh-token-1";
	private static final String OTHER_REFRESH_TOKEN = "refresh-token-2";

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Autowired
	private JwtProperties jwtProperties;

	@BeforeEach
	void setUp() {
		deleteTestKeys();
	}

	@AfterEach
	void tearDown() {
		deleteTestKeys();
	}

	@Test
	void saveAndFindByUserId() {
		refreshTokenService.save(USER_ID, REFRESH_TOKEN);

		Optional<String> savedRefreshToken = refreshTokenService.findByUserId(USER_ID);

		assertThat(savedRefreshToken).contains(REFRESH_TOKEN);
	}

	@Test
	void saveStoresRefreshTokenWithUserIdBasedKey() {
		refreshTokenService.save(USER_ID, REFRESH_TOKEN);

		String savedRefreshToken = redisTemplate.opsForValue().get(refreshTokenKey(USER_ID));

		assertThat(savedRefreshToken).isEqualTo(REFRESH_TOKEN);
		assertThat(redisTemplate.hasKey(refreshTokenKey(EMAIL))).isFalse();
		assertThat(redisTemplate.hasKey(refreshTokenKey(LOGIN_ID))).isFalse();
	}

	@Test
	void saveWithTtlMatchingRefreshTokenValidity() {
		refreshTokenService.save(USER_ID, REFRESH_TOKEN);

		Long ttl = redisTemplate.getExpire(refreshTokenKey(USER_ID), TimeUnit.SECONDS);

		assertThat(ttl).isNotNull();
		assertThat(ttl).isGreaterThan(0L);
		assertThat(ttl).isLessThanOrEqualTo(jwtProperties.refreshTokenValidityInSeconds());
	}

	@Test
	void findByUserIdReadsUserIdBasedKey() {
		redisTemplate.opsForValue().set(refreshTokenKey(USER_ID), REFRESH_TOKEN);
		redisTemplate.opsForValue().set(refreshTokenKey(EMAIL), OTHER_REFRESH_TOKEN);
		redisTemplate.opsForValue().set(refreshTokenKey(LOGIN_ID), OTHER_REFRESH_TOKEN);

		Optional<String> savedRefreshToken = refreshTokenService.findByUserId(USER_ID);

		assertThat(savedRefreshToken).contains(REFRESH_TOKEN);
	}

	@Test
	void deleteThenFindByUserIdReturnsEmpty() {
		refreshTokenService.save(USER_ID, REFRESH_TOKEN);

		refreshTokenService.delete(USER_ID);

		Optional<String> savedRefreshToken = refreshTokenService.findByUserId(USER_ID);
		assertThat(savedRefreshToken).isEmpty();
	}

	@Test
	void deleteRemovesUserIdBasedKeyOnly() {
		refreshTokenService.save(USER_ID, REFRESH_TOKEN);
		redisTemplate.opsForValue().set(refreshTokenKey(EMAIL), OTHER_REFRESH_TOKEN);
		redisTemplate.opsForValue().set(refreshTokenKey(LOGIN_ID), OTHER_REFRESH_TOKEN);

		refreshTokenService.delete(USER_ID);

		assertThat(redisTemplate.hasKey(refreshTokenKey(USER_ID))).isFalse();
		assertThat(redisTemplate.hasKey(refreshTokenKey(EMAIL))).isTrue();
		assertThat(redisTemplate.hasKey(refreshTokenKey(LOGIN_ID))).isTrue();
	}

	@Test
	void matchesReturnsTrueWhenSavedTokenEqualsInputToken() {
		refreshTokenService.save(USER_ID, REFRESH_TOKEN);

		boolean matches = refreshTokenService.matches(USER_ID, REFRESH_TOKEN);

		assertThat(matches).isTrue();
	}

	@Test
	void matchesReturnsFalseWhenInputTokenIsDifferent() {
		refreshTokenService.save(USER_ID, REFRESH_TOKEN);

		boolean matches = refreshTokenService.matches(USER_ID, OTHER_REFRESH_TOKEN);

		assertThat(matches).isFalse();
	}

	@Test
	void matchesReturnsFalseWhenSavedTokenDoesNotExist() {
		boolean matches = refreshTokenService.matches(USER_ID, REFRESH_TOKEN);

		assertThat(matches).isFalse();
	}

	@Test
	void saveThrowsExceptionWhenRefreshTokenIsNull() {
		assertThatThrownBy(() -> refreshTokenService.save(USER_ID, null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void saveThrowsExceptionWhenRefreshTokenIsBlank() {
		assertThatThrownBy(() -> refreshTokenService.save(USER_ID, " "))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void findByUserIdThrowsExceptionWhenUserIdIsNull() {
		assertThatThrownBy(() -> refreshTokenService.findByUserId(null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void deleteDoesNotThrowExceptionWhenKeyDoesNotExist() {
		assertThatCode(() -> refreshTokenService.delete(USER_ID))
			.doesNotThrowAnyException();
	}

	private void deleteTestKeys() {
		redisTemplate.delete(refreshTokenKey(USER_ID));
		redisTemplate.delete(refreshTokenKey(OTHER_USER_ID));
		redisTemplate.delete(refreshTokenKey(EMAIL));
		redisTemplate.delete(refreshTokenKey(LOGIN_ID));
	}

	private String refreshTokenKey(Long userId) {
		return "refresh:" + userId;
	}

	private String refreshTokenKey(String userIdentifier) {
		return "refresh:" + userIdentifier;
	}
}
