package com.neomango.auth.service;

import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neomango.auth.dto.LoginRequest;
import com.neomango.auth.dto.ReissueRequest;
import com.neomango.auth.dto.SignupRequest;
import com.neomango.auth.dto.TokenResponse;
import com.neomango.auth.jwt.JwtProperties;
import com.neomango.auth.jwt.JwtTokenProvider;
import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserStatus;
import com.neomango.user.dto.UserResponse;
import com.neomango.user.policy.UserPolicy;
import com.neomango.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenService refreshTokenService;
	private final JwtProperties jwtProperties;

	public UserResponse signup(SignupRequest request) {
		if (!Objects.equals(request.password(), request.passwordConfirm())) {
			throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
		}

		if (UserPolicy.isReservedNickname(request.nickname())) {
			throw new BusinessException(ErrorCode.RESERVED_NICKNAME);
		}

		if (userRepository.existsByLoginId(request.loginId())) {
			throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
		}

		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		if (userRepository.existsByNickname(request.nickname())) {
			throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
		}

		User user = User.create(
			request.loginId(),
			request.email(),
			passwordEncoder.encode(request.password()),
			request.nickname()
		);

		try {
			return UserResponse.from(userRepository.saveAndFlush(user));
		}
		catch (DataIntegrityViolationException exception) {
			throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
		}
	}

	public TokenResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email())
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

		refreshTokenService.save(user.getId(), refreshToken);

		return new TokenResponse(
			accessToken,
			refreshToken,
			"Bearer",
			jwtProperties.accessTokenValidityInSeconds()
		);
	}

	public TokenResponse reissue(ReissueRequest request) {
		String refreshToken = request.refreshToken();

		if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		Long userId = jwtTokenProvider.getUserId(refreshToken);
		if (!refreshTokenService.matches(userId, refreshToken)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
		String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

		refreshTokenService.save(user.getId(), newRefreshToken);

		return new TokenResponse(
			newAccessToken,
			newRefreshToken,
			"Bearer",
			jwtProperties.accessTokenValidityInSeconds()
		);
	}

	public void logout(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("userId must not be null.");
		}

		refreshTokenService.delete(userId);
	}
}

