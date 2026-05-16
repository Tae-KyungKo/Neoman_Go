package com.neomango.user.dto;

import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;
import com.neomango.user.entity.UserStatus;

public record MeResponse(
	Long id,
	String email,
	String nickname,
	UserRole role,
	UserStatus status
) {

	public static MeResponse from(User user) {
		return new MeResponse(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			user.getRole(),
			user.getStatus()
		);
	}
}
