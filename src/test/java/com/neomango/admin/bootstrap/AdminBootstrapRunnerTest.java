package com.neomango.admin.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class AdminBootstrapRunnerTest {

	private static final String LOGIN_ID = "admin001";
	private static final String EMAIL = "admin@test.com";
	private static final String PASSWORD = "strong-password";
	private static final String NICKNAME = "mangoManager";

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void runDoesNothingWhenBootstrapIsDisabled() {
		AdminBootstrapService service = org.mockito.Mockito.mock(AdminBootstrapService.class);
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
			new AdminBootstrapProperties(false, "", "", "", ""),
			service,
			validator
		);

		runner.run(null);

		verify(service, never()).bootstrap(org.mockito.Mockito.any());
	}

	@Test
	void runDelegatesToServiceWhenBootstrapConfigIsValid() {
		AdminBootstrapService service = org.mockito.Mockito.mock(AdminBootstrapService.class);
		AdminBootstrapProperties properties = properties();
		when(service.bootstrap(properties)).thenReturn(AdminBootstrapResult.created());
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
			properties,
			service,
			validator
		);

		runner.run(null);

		verify(service).bootstrap(properties);
	}

	@Test
	void runFailsWhenRequiredEnvValuesAreMissing() {
		AdminBootstrapService service = org.mockito.Mockito.mock(AdminBootstrapService.class);
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
			new AdminBootstrapProperties(true, "", "", "", ""),
			service,
			validator
		);

		assertThatThrownBy(() -> runner.run(null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("ADMIN_BOOTSTRAP_EMAIL")
			.hasMessageContaining("ADMIN_BOOTSTRAP_LOGIN_ID")
			.hasMessageContaining("ADMIN_BOOTSTRAP_PASSWORD")
			.hasMessageContaining("ADMIN_BOOTSTRAP_NICKNAME");

		verify(service, never()).bootstrap(org.mockito.Mockito.any());
	}

	@Test
	void runFailsWhenLoginIdIsTooShort() {
		assertInvalidLoginId("abc");
	}

	@Test
	void runFailsWhenLoginIdIsTooLong() {
		assertInvalidLoginId("abcdefghijkl3");
	}

	@Test
	void runFailsWhenLoginIdContainsKorean() {
		assertInvalidLoginId("admin한글");
	}

	@Test
	void runFailsWhenLoginIdContainsSpecialCharacter() {
		assertInvalidLoginId("admin001!");
	}

	@Test
	void runFailsWhenLoginIdContainsWhitespace() {
		assertInvalidLoginId("admin 001");
	}

	@Test
	void runFailsWhenEmailIsInvalid() {
		AdminBootstrapService service = org.mockito.Mockito.mock(AdminBootstrapService.class);
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
			new AdminBootstrapProperties(true, LOGIN_ID, "invalid-email", PASSWORD, NICKNAME),
			service,
			validator
		);

		assertThatThrownBy(() -> runner.run(null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("email")
			.hasMessageNotContaining("invalid-email");

		verify(service, never()).bootstrap(org.mockito.Mockito.any());
	}

	@Test
	void runFailsWhenNicknameIsMissing() {
		AdminBootstrapService service = org.mockito.Mockito.mock(AdminBootstrapService.class);
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
			new AdminBootstrapProperties(true, LOGIN_ID, EMAIL, PASSWORD, ""),
			service,
			validator
		);

		assertThatThrownBy(() -> runner.run(null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("ADMIN_BOOTSTRAP_NICKNAME");

		verify(service, never()).bootstrap(org.mockito.Mockito.any());
	}

	@Test
	void runFailsWhenNicknameIsTooShort() {
		assertInvalidNickname("a");
	}

	@Test
	void runFailsWhenNicknameIsTooLong() {
		assertInvalidNickname("abcdefghijkl3");
	}

	@Test
	void runFailsWhenNicknameIsReservedAdministrator() {
		assertInvalidNickname("관리자");
	}

	@Test
	void runFailsWhenNicknameIsReservedOperator() {
		assertInvalidNickname("운영자");
	}

	@Test
	void runFailsWhenNicknameIsReservedAdminUppercase() {
		assertInvalidNickname("ADMIN");
	}

	@Test
	void runFailsWhenNicknameIsReservedAdminMixedCase() {
		assertInvalidNickname("Admin");
	}

	private void assertInvalidLoginId(String loginId) {
		AdminBootstrapService service = org.mockito.Mockito.mock(AdminBootstrapService.class);
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
			new AdminBootstrapProperties(true, loginId, EMAIL, PASSWORD, NICKNAME),
			service,
			validator
		);

		assertThatThrownBy(() -> runner.run(null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("loginId")
			.hasMessageNotContaining(loginId);

		verify(service, never()).bootstrap(org.mockito.Mockito.any());
	}

	private void assertInvalidNickname(String nickname) {
		AdminBootstrapService service = org.mockito.Mockito.mock(AdminBootstrapService.class);
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
			new AdminBootstrapProperties(true, LOGIN_ID, EMAIL, PASSWORD, nickname),
			service,
			validator
		);

		assertThatThrownBy(() -> runner.run(null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("nickname");

		verify(service, never()).bootstrap(org.mockito.Mockito.any());
	}

	private AdminBootstrapProperties properties() {
		return new AdminBootstrapProperties(true, LOGIN_ID, EMAIL, PASSWORD, NICKNAME);
	}
}
