package com.neomango.admin.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class AdminBootstrapRunnerTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void runDoesNothingWhenBootstrapIsDisabled() {
		AdminBootstrapService service = org.mockito.Mockito.mock(AdminBootstrapService.class);
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
			new AdminBootstrapProperties(false, "", "", ""),
			service,
			validator
		);

		runner.run(null);

		verify(service, never()).bootstrap(org.mockito.Mockito.any());
	}

	@Test
	void runFailsWhenRequiredEnvValuesAreMissing() {
		AdminBootstrapService service = org.mockito.Mockito.mock(AdminBootstrapService.class);
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
			new AdminBootstrapProperties(true, "admin@test.com", "", "admin"),
			service,
			validator
		);

		assertThatThrownBy(() -> runner.run(null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("ADMIN_EMAIL")
			.hasMessageContaining("ADMIN_PASSWORD")
			.hasMessageContaining("ADMIN_NICKNAME");

		verify(service, never()).bootstrap(org.mockito.Mockito.any());
	}

	@Test
	void runFailsWhenSignupValidationPolicyIsViolated() {
		AdminBootstrapService service = org.mockito.Mockito.mock(AdminBootstrapService.class);
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
			new AdminBootstrapProperties(true, "admin@test.com", "short", "admin"),
			service,
			validator
		);

		assertThatThrownBy(() -> runner.run(null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("password")
			.hasMessageNotContaining("short");

		verify(service, never()).bootstrap(org.mockito.Mockito.any());
	}
}
