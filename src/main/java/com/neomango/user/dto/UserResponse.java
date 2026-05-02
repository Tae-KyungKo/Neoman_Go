package com.neomango.user.dto;

import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;
import com.neomango.user.entity.UserStatus;

public record UserResponse(
	Long id,
	String email,
	String nickname,
	UserRole role,
	UserStatus status
) {

	public static UserResponse from(User user) {
		return new UserResponse(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			user.getRole(),
			user.getStatus()
		);
	}
}

