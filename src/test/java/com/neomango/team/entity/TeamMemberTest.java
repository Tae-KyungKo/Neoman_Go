package com.neomango.team.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.neomango.team.exception.NotTeamMemberException;
import com.neomango.team.exception.NotTeamOwnerException;
import com.neomango.user.entity.User;

class TeamMemberTest {

	@Test
	void deactivateChangesStatusToInactive() {
		TeamMember teamMember = TeamMember.createMember(team(), user());

		teamMember.deactivate();

		assertThat(teamMember.getStatus()).isEqualTo(TeamMemberStatus.INACTIVE);
		assertThat(teamMember.isActive()).isFalse();
	}

	@Test
	void changeRoleChangesMemberRole() {
		TeamMember teamMember = TeamMember.createMember(team(), user());

		teamMember.changeRole(TeamMemberRole.OWNER);

		assertThat(teamMember.getRole()).isEqualTo(TeamMemberRole.OWNER);
		assertThat(teamMember.isOwner()).isTrue();
	}

	@Test
	void validateActiveThrowsExceptionWhenInactive() {
		TeamMember teamMember = TeamMember.createMember(team(), user());
		teamMember.deactivate();

		assertThatThrownBy(teamMember::validateActive)
			.isInstanceOf(NotTeamMemberException.class);
	}

	@Test
	void validateOwnerThrowsExceptionWhenMember() {
		TeamMember teamMember = TeamMember.createMember(team(), user());

		assertThatThrownBy(teamMember::validateOwner)
			.isInstanceOf(NotTeamOwnerException.class);
	}

	private Team team() {
		Team team = Team.create("Team", null, "GAME", user());
		ReflectionTestUtils.setField(team, "id", 1L);
		return team;
	}

	private User user() {
		User user = User.create(com.neomango.support.TestLoginIds.next(), "user@test.com", "encoded-password", "user");
		ReflectionTestUtils.setField(user, "id", 1L);
		return user;
	}
}
