package com.neomango.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.global.response.ApiResponse;
import com.neomango.global.security.AuthenticatedUser;
import com.neomango.user.service.UserService;
import com.neomango.user.dto.UserResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return ApiResponse.success(UserResponse.from(userService.getById(currentUser.userId())));
	}
}

