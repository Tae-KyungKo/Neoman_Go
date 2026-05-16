package com.neomango.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.neomango.user.entity.UserRole;

@SpringBootTest(properties = {
	"jwt.secret=neomango-test-jwt-secret-key-over-32-bytes",
	"jwt.access-token-validity-in-seconds=1800",
	"jwt.refresh-token-validity-in-seconds=1209600"
})
class JwtTokenProviderTest {

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void createAccessTokenAndValidate() {
		String accessToken = jwtTokenProvider.createAccessToken(1L, UserRole.USER);

		boolean isValid = jwtTokenProvider.validateToken(accessToken);

		assertThat(isValid).isTrue();
	}

	@Test
	void extractUserIdFromAccessToken() {
		String accessToken = jwtTokenProvider.createAccessToken(1L, UserRole.USER);

		Long userId = jwtTokenProvider.getUserId(accessToken);

		assertThat(userId).isEqualTo(1L);
	}

	@Test
	void extractRoleFromAccessToken() {
		String accessToken = jwtTokenProvider.createAccessToken(1L, UserRole.ADMIN);

		UserRole role = jwtTokenProvider.getRole(accessToken);

		assertThat(role).isEqualTo(UserRole.ADMIN);
	}

	@Test
	void createRefreshTokenAndValidate() {
		String refreshToken = jwtTokenProvider.createRefreshToken(1L);

		boolean isValid = jwtTokenProvider.validateToken(refreshToken);

		assertThat(isValid).isTrue();
	}

	@Test
	void refreshTokenHasRefreshTokenType() {
		String refreshToken = jwtTokenProvider.createRefreshToken(1L);

		boolean isRefreshToken = jwtTokenProvider.isRefreshToken(refreshToken);

		assertThat(isRefreshToken).isTrue();
	}

	@Test
	void invalidTokenReturnsFalse() {
		boolean isValid = jwtTokenProvider.validateToken("invalid-token");

		assertThat(isValid).isFalse();
	}

	@Test
	void expiredTokenReturnsFalse() {
		JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(
			new JwtProperties("neomango-test-jwt-secret-key-over-32-bytes", -1, 1209600)
		);
		String expiredToken = expiredTokenProvider.createAccessToken(1L, UserRole.USER);

		boolean isValid = expiredTokenProvider.validateToken(expiredToken);

		assertThat(isValid).isFalse();
	}
}
