package com.neomango.support;

import org.springframework.test.util.ReflectionTestUtils;

import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;

public final class UserTestFixture {

	private static final String ENCODED_PASSWORD = "encoded-password";

	private UserTestFixture() {
	}

	public static User user(Long id, String email, String nickname) {
		User user = User.create(email, ENCODED_PASSWORD, nickname);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	public static User admin(Long id, String email, String nickname) {
		User user = user(id, email, nickname);
		ReflectionTestUtils.setField(user, "role", UserRole.ADMIN);
		return user;
	}
}
