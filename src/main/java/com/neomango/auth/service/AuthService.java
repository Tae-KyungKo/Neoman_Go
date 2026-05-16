package com.neomango.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neomango.auth.dto.LoginRequest;
import com.neomango.auth.dto.SignupRequest;
import com.neomango.auth.dto.TokenResponse;
import com.neomango.auth.jwt.JwtProperties;
import com.neomango.auth.jwt.JwtTokenProvider;
import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserStatus;
import com.neomango.user.dto.UserResponse;
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
		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		User user = User.create(
			request.email(),
			passwordEncoder.encode(request.password()),
			request.nickname()
		);

		return UserResponse.from(userRepository.save(user));
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
}

