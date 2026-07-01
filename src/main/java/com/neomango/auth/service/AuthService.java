package com.neomango.auth.service;

import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.neomango.auth.dto.AvailabilityCheckResult;
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

	// UX helper only. Signup keeps final duplicate validation and DB unique constraints are the last guard.
	// TODO(Phase 9 hardening): Consider rate limiting or CAPTCHA to reduce user enumeration.
	@Transactional(readOnly = true)
	public AvailabilityCheckResult checkLoginIdAvailability(String loginId) {
		if (!StringUtils.hasText(loginId) || !UserPolicy.LOGIN_ID_REGEX.matcher(loginId).matches()) {
			return new AvailabilityCheckResult(false, "아이디는 4~12자의 영문 대소문자와 숫자만 사용할 수 있습니다.");
		}

		if (userRepository.existsByLoginId(loginId)) {
			return new AvailabilityCheckResult(false, "이미 존재하는 아이디입니다.");
		}

		return new AvailabilityCheckResult(true, "사용 가능한 아이디입니다.");
	}

	@Transactional(readOnly = true)
	public AvailabilityCheckResult checkNicknameAvailability(String nickname) {
		if (!StringUtils.hasText(nickname)
			|| nickname.length() < UserPolicy.NICKNAME_MIN_LENGTH
			|| nickname.length() > UserPolicy.NICKNAME_MAX_LENGTH) {
			return new AvailabilityCheckResult(false, "닉네임은 2~12자여야 합니다.");
		}

		if (UserPolicy.isReservedNickname(nickname)) {
			return new AvailabilityCheckResult(false, "사용할 수 없는 닉네임입니다.");
		}

		if (userRepository.existsByNickname(nickname)) {
			return new AvailabilityCheckResult(false, "이미 존재하는 닉네임입니다.");
		}

		return new AvailabilityCheckResult(true, "사용 가능한 닉네임입니다.");
	}

	@Transactional(readOnly = true)
	public boolean isLoginIdAvailable(String loginId) {
		return checkLoginIdAvailability(loginId).available();
	}

	@Transactional(readOnly = true)
	public boolean isNicknameAvailable(String nickname) {
		return checkNicknameAvailability(nickname).available();
	}

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
		User user = userRepository.findByLoginId(request.loginId())
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

