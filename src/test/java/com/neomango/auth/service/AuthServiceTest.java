package com.neomango.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.neomango.auth.dto.LoginRequest;
import com.neomango.auth.dto.ReissueRequest;
import com.neomango.auth.dto.TokenResponse;
import com.neomango.auth.jwt.JwtProperties;
import com.neomango.auth.jwt.JwtTokenProvider;
import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;
import com.neomango.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final Long USER_ID = 1L;
	private static final String EMAIL = "user@test.com";
	private static final String RAW_PASSWORD = "raw-password";
	private static final String ENCODED_PASSWORD = "encoded-password";
	private static final String ACCESS_TOKEN = "access-token";
	private static final String REFRESH_TOKEN = "refresh-token";
	private static final String OLD_REFRESH_TOKEN = "old-refresh-token";
	private static final String NEW_ACCESS_TOKEN = "new-access-token";
	private static final String NEW_REFRESH_TOKEN = "new-refresh-token";
	private static final long ACCESS_TOKEN_EXPIRES_IN = 1800L;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private RefreshTokenService refreshTokenService;

	@Mock
	private JwtProperties jwtProperties;

	@InjectMocks
	private AuthService authService;

	@Test
	void loginReturnsAccessTokenAndRefreshTokenWhenCredentialsAreValid() {
		User user = activeUser();
		LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
		when(jwtTokenProvider.createAccessToken(USER_ID, UserRole.USER)).thenReturn(ACCESS_TOKEN);
		when(jwtTokenProvider.createRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);
		when(jwtProperties.accessTokenValidityInSeconds()).thenReturn(ACCESS_TOKEN_EXPIRES_IN);

		TokenResponse response = authService.login(request);

		assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
		assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.accessTokenExpiresIn()).isEqualTo(ACCESS_TOKEN_EXPIRES_IN);
	}

	@Test
	void loginSavesRefreshTokenWhenCredentialsAreValid() {
		User user = activeUser();
		LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
		when(jwtTokenProvider.createAccessToken(USER_ID, UserRole.USER)).thenReturn(ACCESS_TOKEN);
		when(jwtTokenProvider.createRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);
		when(jwtProperties.accessTokenValidityInSeconds()).thenReturn(ACCESS_TOKEN_EXPIRES_IN);

		authService.login(request);

		verify(refreshTokenService).save(USER_ID, REFRESH_TOKEN);
	}

	@Test
	void loginThrowsExceptionWhenEmailDoesNotExist() {
		LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	void loginThrowsExceptionWhenPasswordDoesNotMatch() {
		User user = activeUser();
		LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	void loginThrowsExceptionWhenUserIsDeleted() {
		User user = activeUser();
		user.softDelete();
		LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	void loginUsesPasswordEncoderMatches() {
		User user = activeUser();
		LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BusinessException.class);

		verify(passwordEncoder).matches(RAW_PASSWORD, ENCODED_PASSWORD);
	}

	@Test
	void loginCreatesAccessTokenAndRefreshToken() {
		User user = activeUser();
		LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
		when(jwtTokenProvider.createAccessToken(USER_ID, UserRole.USER)).thenReturn(ACCESS_TOKEN);
		when(jwtTokenProvider.createRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);
		when(jwtProperties.accessTokenValidityInSeconds()).thenReturn(ACCESS_TOKEN_EXPIRES_IN);

		authService.login(request);

		verify(jwtTokenProvider).createAccessToken(USER_ID, UserRole.USER);
		verify(jwtTokenProvider).createRefreshToken(USER_ID);
	}

	@Test
	void loginDoesNotCreateTokenWhenEmailDoesNotExist() {
		LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BusinessException.class);

		verifyNoInteractions(jwtTokenProvider, refreshTokenService);
	}

	@Test
	void reissueReturnsNewAccessTokenAndRefreshTokenWhenRefreshTokenMatchesRedisToken() {
		User user = activeUser();
		ReissueRequest request = new ReissueRequest(OLD_REFRESH_TOKEN);
		when(jwtTokenProvider.validateToken(OLD_REFRESH_TOKEN)).thenReturn(true);
		when(jwtTokenProvider.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(true);
		when(jwtTokenProvider.getUserId(OLD_REFRESH_TOKEN)).thenReturn(USER_ID);
		when(refreshTokenService.matches(USER_ID, OLD_REFRESH_TOKEN)).thenReturn(true);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(jwtTokenProvider.createAccessToken(USER_ID, UserRole.USER)).thenReturn(NEW_ACCESS_TOKEN);
		when(jwtTokenProvider.createRefreshToken(USER_ID)).thenReturn(NEW_REFRESH_TOKEN);
		when(jwtProperties.accessTokenValidityInSeconds()).thenReturn(ACCESS_TOKEN_EXPIRES_IN);

		TokenResponse response = authService.reissue(request);

		assertThat(response.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
		assertThat(response.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.accessTokenExpiresIn()).isEqualTo(ACCESS_TOKEN_EXPIRES_IN);
	}

	@Test
	void reissueSavesNewRefreshTokenWhenRefreshTokenMatchesRedisToken() {
		User user = activeUser();
		ReissueRequest request = new ReissueRequest(OLD_REFRESH_TOKEN);
		when(jwtTokenProvider.validateToken(OLD_REFRESH_TOKEN)).thenReturn(true);
		when(jwtTokenProvider.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(true);
		when(jwtTokenProvider.getUserId(OLD_REFRESH_TOKEN)).thenReturn(USER_ID);
		when(refreshTokenService.matches(USER_ID, OLD_REFRESH_TOKEN)).thenReturn(true);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(jwtTokenProvider.createAccessToken(USER_ID, UserRole.USER)).thenReturn(NEW_ACCESS_TOKEN);
		when(jwtTokenProvider.createRefreshToken(USER_ID)).thenReturn(NEW_REFRESH_TOKEN);
		when(jwtProperties.accessTokenValidityInSeconds()).thenReturn(ACCESS_TOKEN_EXPIRES_IN);

		authService.reissue(request);

		verify(refreshTokenService).save(USER_ID, NEW_REFRESH_TOKEN);
	}

	@Test
	void reissueThrowsExceptionWhenAccessTokenIsUsed() {
		ReissueRequest request = new ReissueRequest(ACCESS_TOKEN);
		when(jwtTokenProvider.validateToken(ACCESS_TOKEN)).thenReturn(true);
		when(jwtTokenProvider.isRefreshToken(ACCESS_TOKEN)).thenReturn(false);

		assertThatThrownBy(() -> authService.reissue(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	void reissueThrowsExceptionWhenRefreshTokenIsInvalid() {
		ReissueRequest request = new ReissueRequest(OLD_REFRESH_TOKEN);
		when(jwtTokenProvider.validateToken(OLD_REFRESH_TOKEN)).thenReturn(false);

		assertThatThrownBy(() -> authService.reissue(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	void reissueThrowsExceptionWhenSavedRefreshTokenDoesNotExist() {
		ReissueRequest request = new ReissueRequest(OLD_REFRESH_TOKEN);
		when(jwtTokenProvider.validateToken(OLD_REFRESH_TOKEN)).thenReturn(true);
		when(jwtTokenProvider.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(true);
		when(jwtTokenProvider.getUserId(OLD_REFRESH_TOKEN)).thenReturn(USER_ID);
		when(refreshTokenService.matches(USER_ID, OLD_REFRESH_TOKEN)).thenReturn(false);

		assertThatThrownBy(() -> authService.reissue(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	void reissueThrowsExceptionWhenRedisTokenDoesNotMatchRequestToken() {
		ReissueRequest request = new ReissueRequest(OLD_REFRESH_TOKEN);
		when(jwtTokenProvider.validateToken(OLD_REFRESH_TOKEN)).thenReturn(true);
		when(jwtTokenProvider.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(true);
		when(jwtTokenProvider.getUserId(OLD_REFRESH_TOKEN)).thenReturn(USER_ID);
		when(refreshTokenService.matches(USER_ID, OLD_REFRESH_TOKEN)).thenReturn(false);

		assertThatThrownBy(() -> authService.reissue(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	void reissueThrowsExceptionWhenUserDoesNotExist() {
		ReissueRequest request = new ReissueRequest(OLD_REFRESH_TOKEN);
		when(jwtTokenProvider.validateToken(OLD_REFRESH_TOKEN)).thenReturn(true);
		when(jwtTokenProvider.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(true);
		when(jwtTokenProvider.getUserId(OLD_REFRESH_TOKEN)).thenReturn(USER_ID);
		when(refreshTokenService.matches(USER_ID, OLD_REFRESH_TOKEN)).thenReturn(true);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.reissue(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	void reissueThrowsExceptionWhenUserIsDeleted() {
		User user = activeUser();
		user.softDelete();
		ReissueRequest request = new ReissueRequest(OLD_REFRESH_TOKEN);
		when(jwtTokenProvider.validateToken(OLD_REFRESH_TOKEN)).thenReturn(true);
		when(jwtTokenProvider.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(true);
		when(jwtTokenProvider.getUserId(OLD_REFRESH_TOKEN)).thenReturn(USER_ID);
		when(refreshTokenService.matches(USER_ID, OLD_REFRESH_TOKEN)).thenReturn(true);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.reissue(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	void reissueAppliesRefreshTokenRotation() {
		User user = activeUser();
		ReissueRequest request = new ReissueRequest(OLD_REFRESH_TOKEN);
		when(jwtTokenProvider.validateToken(OLD_REFRESH_TOKEN)).thenReturn(true);
		when(jwtTokenProvider.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(true);
		when(jwtTokenProvider.getUserId(OLD_REFRESH_TOKEN)).thenReturn(USER_ID);
		when(refreshTokenService.matches(USER_ID, OLD_REFRESH_TOKEN)).thenReturn(true);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(jwtTokenProvider.createAccessToken(USER_ID, UserRole.USER)).thenReturn(NEW_ACCESS_TOKEN);
		when(jwtTokenProvider.createRefreshToken(USER_ID)).thenReturn(NEW_REFRESH_TOKEN);
		when(jwtProperties.accessTokenValidityInSeconds()).thenReturn(ACCESS_TOKEN_EXPIRES_IN);

		TokenResponse response = authService.reissue(request);

		assertThat(response.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
		assertThat(response.refreshToken()).isNotEqualTo(OLD_REFRESH_TOKEN);
		verify(refreshTokenService).save(USER_ID, NEW_REFRESH_TOKEN);
	}

	@Test
	void logoutDeletesRefreshToken() {
		authService.logout(USER_ID);

		verify(refreshTokenService).delete(USER_ID);
	}

	@Test
	void logoutThrowsExceptionWhenUserIdIsNull() {
		assertThatThrownBy(() -> authService.logout(null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void logoutSucceedsEvenWhenRefreshTokenDoesNotExist() {
		authService.logout(USER_ID);

		verify(refreshTokenService).delete(USER_ID);
	}

	private User activeUser() {
		User user = User.create(com.neomango.support.TestLoginIds.next(), EMAIL, ENCODED_PASSWORD, "nickname");
		ReflectionTestUtils.setField(user, "id", USER_ID);
		return user;
	}
}
