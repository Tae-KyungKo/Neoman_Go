package com.neomango.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import com.neomango.user.policy.UserPolicy;

public record LoginRequest(
	@NotBlank
	@Pattern(regexp = UserPolicy.LOGIN_ID_PATTERN)
	String loginId,

	@NotBlank
	String password
) {
}

