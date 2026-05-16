package com.neomango.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class RedisConnectionTest {

	private static final String REFRESH_TOKEN_KEY = "RT:1";
	private static final String REFRESH_TOKEN_VALUE = "test-refresh-token";

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Test
	void redisStringValueWithTtl() {
		try {
			redisTemplate.opsForValue().set(REFRESH_TOKEN_KEY, REFRESH_TOKEN_VALUE, Duration.ofSeconds(10));

			String savedValue = redisTemplate.opsForValue().get(REFRESH_TOKEN_KEY);
			Long ttl = redisTemplate.getExpire(REFRESH_TOKEN_KEY, TimeUnit.SECONDS);

			assertThat(savedValue).isEqualTo(REFRESH_TOKEN_VALUE);
			assertThat(ttl).isNotNull();
			assertThat(ttl).isGreaterThan(0L);
		} finally {
			redisTemplate.delete(REFRESH_TOKEN_KEY);
		}
	}
}
