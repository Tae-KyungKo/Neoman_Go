package com.neomango.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.neomango.auth.dto.LoginRequest;
import com.neomango.auth.dto.ReissueRequest;
import com.neomango.auth.service.AuthService;
import com.neomango.auth.dto.SignupRequest;
import com.neomango.auth.dto.TokenResponse;
import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.global.response.ApiResponse;
import com.neomango.global.security.AuthenticatedUser;
import com.neomango.user.dto.UserResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/signup")
	public ApiResponse<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
		return ApiResponse.success(authService.signup(request));
	}

	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(authService.login(request));
	}

	@PostMapping("/reissue")
	public ApiResponse<TokenResponse> reissue(@Valid @RequestBody ReissueRequest request) {
		return ApiResponse.success(authService.reissue(request));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(@AuthenticationPrincipal AuthenticatedUser currentUser) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		authService.logout(currentUser.userId());
		return ApiResponse.successWithoutData();
	}
}

