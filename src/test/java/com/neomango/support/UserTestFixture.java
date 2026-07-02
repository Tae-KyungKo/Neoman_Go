package com.neomango.support;

import org.springframework.test.util.ReflectionTestUtils;

import com.neomango.user.entity.User;

public final class UserTestFixture {

	private static final String ENCODED_PASSWORD = "encoded-password";

	private UserTestFixture() {
	}

	public static User user(Long id, String email, String nickname) {
		User user = User.create(com.neomango.support.TestLoginIds.next(), email, ENCODED_PASSWORD, nickname);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	public static User admin(Long id, String email, String nickname) {
		User user = User.createAdmin(com.neomango.support.TestLoginIds.next(), email, ENCODED_PASSWORD, nickname);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
