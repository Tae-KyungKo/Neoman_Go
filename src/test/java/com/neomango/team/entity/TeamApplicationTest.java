package com.neomango.team.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.neomango.team.exception.ApplicationAlreadyProcessedException;
import com.neomango.user.entity.User;

class TeamApplicationTest {

	@Test
	void createInitializesPendingAndActive() {
		TeamApplication application = application();

		assertThat(application.getStatus()).isEqualTo(TeamApplicationStatus.PENDING);
		assertThat(application.isActive()).isTrue();
	}

	@Test
	void approveChangesStatusAndDeactivatesApplication() {
		TeamApplication application = application();

		application.approve();

		assertThat(application.getStatus()).isEqualTo(TeamApplicationStatus.APPROVED);
		assertThat(application.isActive()).isFalse();
	}

	@Test
	void rejectChangesStatusAndDeactivatesApplication() {
		TeamApplication application = application();

		application.reject();

		assertThat(application.getStatus()).isEqualTo(TeamApplicationStatus.REJECTED);
		assertThat(application.isActive()).isFalse();
	}

	@Test
	void cancelChangesStatusAndDeactivatesApplication() {
		TeamApplication application = application();

		application.cancel();

		assertThat(application.getStatus()).isEqualTo(TeamApplicationStatus.CANCELED);
		assertThat(application.isActive()).isFalse();
		assertThat(application.getCanceledAt()).isNotNull();
	}

	@Test
	void approveRejectAndCancelFailWhenApplicationIsNotPending() {
		TeamApplication approved = application();
		approved.approve();
		TeamApplication rejected = application();
		rejected.reject();
		TeamApplication canceled = application();
		canceled.cancel();

		assertAlreadyProcessed(approved::approve);
		assertAlreadyProcessed(rejected::reject);
		assertAlreadyProcessed(canceled::cancel);
	}

	@Test
	void validatePendingFailsWhenApplicationIsAlreadyProcessed() {
		TeamApplication application = application();
		application.approve();

		assertAlreadyProcessed(application::validatePending);
	}

	private void assertAlreadyProcessed(Runnable runnable) {
		assertThatThrownBy(runnable::run)
			.isInstanceOf(ApplicationAlreadyProcessedException.class);
	}

	private TeamApplication application() {
		User owner = User.create("owner@test.com", "encoded-password", "owner");
		User applicant = User.create("applicant@test.com", "encoded-password", "applicant");
		Team team = Team.create("Team", null, "FUTSAL", owner);
		return TeamApplication.create(team, applicant, "message");
	}
}
