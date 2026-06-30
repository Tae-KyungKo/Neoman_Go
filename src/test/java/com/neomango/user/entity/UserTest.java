package com.neomango.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

	@Test
	void createStoresLoginIdAndUserDefaults() {
		User user = User.create("tester01", "tester@test.com", "encoded-password", "tester");

		assertThat(user.getLoginId()).isEqualTo("tester01");
		assertThat(user.getEmail()).isEqualTo("tester@test.com");
		assertThat(user.getPassword()).isEqualTo("encoded-password");
		assertThat(user.getNickname()).isEqualTo("tester");
		assertThat(user.getRole()).isEqualTo(UserRole.USER);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void createAdminStoresLoginIdAndAdminRole() {
		User admin = User.createAdmin("admin01", "admin@test.com", "encoded-password", "admin");

		assertThat(admin.getLoginId()).isEqualTo("admin01");
		assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
		assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}
}
