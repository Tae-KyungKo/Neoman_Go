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

	private User activeUser() {
		User user = User.create(EMAIL, ENCODED_PASSWORD, "nickname");
		ReflectionTestUtils.setField(user, "id", USER_ID);
		return user;
	}
}
