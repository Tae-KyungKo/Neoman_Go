package com.neomango.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.neomango.user.policy.UserPolicy;

public record SignupRequest(
	@NotBlank
	@Pattern(regexp = UserPolicy.LOGIN_ID_PATTERN)
	String loginId,

	@NotBlank
	@Pattern(regexp = UserPolicy.PASSWORD_PATTERN)
	String password,

	@NotBlank
	String passwordConfirm,

	@NotBlank
	@Email
	String email,

	@NotBlank
	@Size(min = UserPolicy.NICKNAME_MIN_LENGTH, max = UserPolicy.NICKNAME_MAX_LENGTH)
	String nickname
) {

	@Deprecated
	public SignupRequest(String email, String password, String nickname) {
		// TODO(Phase 9-9): Remove this AdminBootstrap compatibility constructor after bootstrap accepts loginId.
		this("admin01", password, password, email, nickname);
	}
}

