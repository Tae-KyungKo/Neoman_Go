package com.neomango.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.neomango.notification.service.NotificationService;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamMemberRole;
import com.neomango.team.entity.TeamMemberStatus;
import com.neomango.team.entity.TeamStatus;
import com.neomango.team.exception.CannotLeaveOwnerWithoutDelegationException;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
import com.neomango.team.repository.UserCategoryMembershipRepository;
import com.neomango.user.entity.User;

@ExtendWith(MockitoExtension.class)
class TeamMemberServiceTest {

	private static final Long TEAM_ID = 10L;
	private static final Long OWNER_ID = 1L;
	private static final Long MEMBER_ID = 2L;
	private static final Long OTHER_MEMBER_ID = 3L;

	@Mock
	private TeamRepository teamRepository;

	@Mock
	private TeamApplicationRepository teamApplicationRepository;

	@Mock
	private TeamMemberRepository teamMemberRepository;

	@Mock
	private UserCategoryMembershipRepository userCategoryMembershipRepository;

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private TeamMemberService teamMemberService;

	@Test
	void leaveTeamCreatesMemberLeftNotificationsForRemainingActiveMembers() {
		User owner = user(OWNER_ID, "owner@test.com", "owner");
		User member = user(MEMBER_ID, "member@test.com", "member");
		User otherMember = user(OTHER_MEMBER_ID, "other@test.com", "other");
		Team team = team(owner);
		TeamMember ownerMember = team.getMembers().get(0);
		TeamMember memberTeamMember = TeamMember.createMember(team, member);
		TeamMember otherTeamMember = TeamMember.createMember(team, otherMember);
		team.addMember(memberTeamMember);
		team.addMember(otherTeamMember);
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(TEAM_ID, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.findActiveMemberByTeamIdAndUserId(TEAM_ID, MEMBER_ID))
			.thenReturn(Optional.of(memberTeamMember));
		when(teamMemberRepository.findActiveMembersByTeamId(TEAM_ID))
			.thenReturn(List.of(ownerMember, otherTeamMember));

		teamMemberService.leaveTeam(TEAM_ID, MEMBER_ID);

		assertThat(memberTeamMember.getStatus()).isEqualTo(TeamMemberStatus.INACTIVE);
		verify(userCategoryMembershipRepository).deleteByUserIdAndCategory(MEMBER_ID, "GAME");
		verify(notificationService).createTeamMemberLeftNotification(
			OWNER_ID,
			MEMBER_ID,
			"Game Team",
			"member",
			TEAM_ID
		);
		verify(notificationService).createTeamMemberLeftNotification(
			OTHER_MEMBER_ID,
			MEMBER_ID,
			"Game Team",
			"member",
			TEAM_ID
		);
	}

	@Test
	void leaveTeamDoesNotCreateMemberLeftNotificationWhenThereIsNoRemainingActiveMember() {
		User owner = user(OWNER_ID, "owner@test.com", "owner");
		Team team = team(owner);
		TeamMember ownerMember = team.getMembers().get(0);
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(TEAM_ID, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.findActiveMemberByTeamIdAndUserId(TEAM_ID, OWNER_ID))
			.thenReturn(Optional.of(ownerMember));
		when(teamMemberRepository.countActiveMembersByTeamId(TEAM_ID)).thenReturn(1L);
		when(teamApplicationRepository.findByTeamIdAndStatusOrderByCreatedAtAsc(TEAM_ID, com.neomango.team.entity.TeamApplicationStatus.PENDING))
			.thenReturn(List.of());

		teamMemberService.leaveTeam(TEAM_ID, OWNER_ID);

		verify(notificationService, never()).createTeamMemberLeftNotification(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
	}

	@Test
	void leaveTeamDoesNotCreateMemberLeftNotificationWhenOwnerCannotLeaveWithoutDelegation() {
		User owner = user(OWNER_ID, "owner@test.com", "owner");
		User member = user(MEMBER_ID, "member@test.com", "member");
		Team team = team(owner);
		team.addMember(TeamMember.createMember(team, member));
		TeamMember ownerMember = team.getMembers().get(0);
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(TEAM_ID, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.findActiveMemberByTeamIdAndUserId(TEAM_ID, OWNER_ID))
			.thenReturn(Optional.of(ownerMember));
		when(teamMemberRepository.countActiveMembersByTeamId(TEAM_ID)).thenReturn(2L);

		assertThatThrownBy(() -> teamMemberService.leaveTeam(TEAM_ID, OWNER_ID))
			.isInstanceOf(CannotLeaveOwnerWithoutDelegationException.class);
		verify(notificationService, never()).createTeamMemberLeftNotification(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
	}

	@Test
	void kickMemberCreatesMemberKickedNotificationOnlyForTarget() {
		User owner = user(OWNER_ID, "owner@test.com", "owner");
		User target = user(MEMBER_ID, "target@test.com", "target");
		Team team = team(owner);
		TeamMember ownerMember = team.getMembers().get(0);
		ReflectionTestUtils.setField(ownerMember, "id", 10L);
		TeamMember targetMember = TeamMember.createMember(team, target);
		ReflectionTestUtils.setField(targetMember, "id", 20L);
		team.addMember(targetMember);
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(TEAM_ID, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.findActiveMemberByTeamIdAndUserId(TEAM_ID, OWNER_ID))
			.thenReturn(Optional.of(ownerMember));
		when(teamMemberRepository.findActiveMemberById(20L)).thenReturn(Optional.of(targetMember));

		teamMemberService.kickMember(TEAM_ID, 20L, OWNER_ID);

		assertThat(targetMember.getStatus()).isEqualTo(TeamMemberStatus.INACTIVE);
		verify(userCategoryMembershipRepository).deleteByUserIdAndCategory(MEMBER_ID, "GAME");
		verify(notificationService).createTeamMemberKickedNotification(
			MEMBER_ID,
			OWNER_ID,
			"Game Team",
			TEAM_ID
		);
		verify(notificationService, never()).createTeamMemberLeftNotification(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
	}

	@Test
	void delegateOwnerCreatesOwnerDelegatedNotificationOnlyForNewOwner() {
		User owner = user(OWNER_ID, "owner@test.com", "owner");
		User target = user(MEMBER_ID, "target@test.com", "target");
		Team team = team(owner);
		TeamMember ownerMember = team.getMembers().get(0);
		ReflectionTestUtils.setField(ownerMember, "id", 10L);
		TeamMember targetMember = TeamMember.createMember(team, target);
		ReflectionTestUtils.setField(targetMember, "id", 20L);
		team.addMember(targetMember);
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(TEAM_ID, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.findActiveMemberByTeamIdAndUserId(TEAM_ID, OWNER_ID))
			.thenReturn(Optional.of(ownerMember));
		when(teamMemberRepository.findActiveMemberById(20L)).thenReturn(Optional.of(targetMember));
		when(teamMemberRepository.countActiveOwnersByTeamId(TEAM_ID)).thenReturn(1L);

		teamMemberService.delegateOwner(TEAM_ID, OWNER_ID, 20L);

		assertThat(ownerMember.getRole()).isEqualTo(TeamMemberRole.MEMBER);
		assertThat(targetMember.getRole()).isEqualTo(TeamMemberRole.OWNER);
		verify(notificationService).createTeamOwnerDelegatedNotification(
			MEMBER_ID,
			OWNER_ID,
			"Game Team",
			TEAM_ID
		);
	}

	private Team team(User owner) {
		Team team = Team.create("Game Team", null, "GAME", owner);
		ReflectionTestUtils.setField(team, "id", TEAM_ID);
		return team;
	}

	private User user(Long userId, String email, String nickname) {
		User user = User.create(email, "encoded-password", nickname);
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}
}
