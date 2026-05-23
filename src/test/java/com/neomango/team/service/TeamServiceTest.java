package com.neomango.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.team.dto.TeamCreateRequest;
import com.neomango.team.dto.TeamDetailResponse;
import com.neomango.team.dto.TeamResponse;
import com.neomango.team.dto.TeamSummaryResponse;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamMemberRole;
import com.neomango.team.entity.TeamMemberStatus;
import com.neomango.team.entity.TeamStatus;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
import com.neomango.user.entity.User;
import com.neomango.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

	private static final Long USER_ID = 1L;

	@Mock
	private TeamRepository teamRepository;

	@Mock
	private TeamMemberRepository teamMemberRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private TeamService teamService;

	@Test
	void createTeamReturnsTeamResponse() {
		User owner = activeUser();
		TeamCreateRequest request = request();
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
		when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
			Team team = invocation.getArgument(0);
			ReflectionTestUtils.setField(team, "id", 10L);
			return team;
		});

		TeamResponse response = teamService.createTeam(USER_ID, request);

		assertThat(response.id()).isEqualTo(10L);
		assertThat(response.name()).isEqualTo("Game Team");
		assertThat(response.description()).isEqualTo("Weekend game team");
		assertThat(response.category()).isEqualTo("GAME");
		assertThat(response.status()).isEqualTo(TeamStatus.RECRUITING);
		assertThat(response.ownerId()).isEqualTo(USER_ID);
		assertThat(response.ownerNickname()).isEqualTo("owner");
	}

	@Test
	void createTeamSavesOwnerTeamMember() {
		User owner = activeUser();
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
		when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

		teamService.createTeam(USER_ID, request());

		ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
		verify(teamRepository).save(teamCaptor.capture());
		Team savedTeam = teamCaptor.getValue();
		assertThat(savedTeam.getMembers()).hasSize(1);
		TeamMember ownerMember = savedTeam.getMembers().get(0);
		assertThat(ownerMember.getTeam()).isSameAs(savedTeam);
		assertThat(ownerMember.getUser()).isSameAs(owner);
		assertThat(ownerMember.getRole()).isEqualTo(TeamMemberRole.OWNER);
	}

	@Test
	void createTeamThrowsExceptionWhenUserDoesNotExist() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> teamService.createTeam(USER_ID, request()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	void createTeamThrowsExceptionWhenUserIsDeleted() {
		User owner = activeUser();
		owner.softDelete();
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));

		assertThatThrownBy(() -> teamService.createTeam(USER_ID, request()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	void getTeamsReturnsTeamSummaries() {
		User owner = activeUser();
		Team team = savedTeam(10L, owner, "GAME");
		TeamMember ownerMember = team.getMembers().get(0);
		PageRequest pageable = PageRequest.of(0, 20);
		when(teamRepository.findByDeletedAtIsNull(pageable))
			.thenReturn(new PageImpl<>(List.of(team), pageable, 1));
		when(teamMemberRepository.findOwnersByTeamIds(List.of(10L))).thenReturn(List.of(ownerMember));

		Page<TeamSummaryResponse> response = teamService.getTeams(null, pageable);

		assertThat(response.getContent()).hasSize(1);
		TeamSummaryResponse summary = response.getContent().get(0);
		assertThat(summary.id()).isEqualTo(10L);
		assertThat(summary.ownerId()).isEqualTo(USER_ID);
		assertThat(summary.ownerNickname()).isEqualTo("owner");
	}

	@Test
	void getTeamsReturnsTeamSummariesFilteredByCategory() {
		User owner = activeUser();
		Team team = savedTeam(11L, owner, "FUTSAL");
		TeamMember ownerMember = team.getMembers().get(0);
		PageRequest pageable = PageRequest.of(0, 20);
		when(teamRepository.findByCategoryAndDeletedAtIsNull("FUTSAL", pageable))
			.thenReturn(new PageImpl<>(List.of(team), pageable, 1));
		when(teamMemberRepository.findOwnersByTeamIds(List.of(11L))).thenReturn(List.of(ownerMember));

		Page<TeamSummaryResponse> response = teamService.getTeams("FUTSAL", pageable);

		assertThat(response.getContent()).hasSize(1);
		assertThat(response.getContent().get(0).category()).isEqualTo("FUTSAL");
	}

	@Test
	void getTeamsTrimsCategoryFilter() {
		User owner = activeUser();
		Team team = savedTeam(13L, owner, "FUTSAL");
		TeamMember ownerMember = team.getMembers().get(0);
		PageRequest pageable = PageRequest.of(0, 20);
		when(teamRepository.findByCategoryAndDeletedAtIsNull("FUTSAL", pageable))
			.thenReturn(new PageImpl<>(List.of(team), pageable, 1));
		when(teamMemberRepository.findOwnersByTeamIds(List.of(13L))).thenReturn(List.of(ownerMember));

		Page<TeamSummaryResponse> response = teamService.getTeams(" FUTSAL ", pageable);

		assertThat(response.getContent()).hasSize(1);
		assertThat(response.getContent().get(0).category()).isEqualTo("FUTSAL");
	}

	@Test
	void getTeamsThrowsExceptionWhenOwnerIsMissing() {
		User owner = activeUser();
		Team team = savedTeam(14L, owner, "GAME");
		PageRequest pageable = PageRequest.of(0, 20);
		when(teamRepository.findByDeletedAtIsNull(pageable))
			.thenReturn(new PageImpl<>(List.of(team), pageable, 1));
		when(teamMemberRepository.findOwnersByTeamIds(List.of(14L))).thenReturn(List.of());

		assertThatThrownBy(() -> teamService.getTeams(null, pageable))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.TEAM_OWNER_NOT_FOUND);
	}

	@Test
	void getTeamDetailReturnsTeamMembers() {
		User owner = activeUser();
		Team team = savedTeam(12L, owner, "GAME");
		TeamMember ownerMember = team.getMembers().get(0);
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(12L, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.findByTeamIdWithUser(12L)).thenReturn(List.of(ownerMember));

		TeamDetailResponse response = teamService.getTeamDetail(12L);

		assertThat(response.id()).isEqualTo(12L);
		assertThat(response.owner().userId()).isEqualTo(USER_ID);
		assertThat(response.owner().role()).isEqualTo(TeamMemberRole.OWNER);
		assertThat(response.members()).hasSize(1);
		assertThat(response.members().get(0).nickname()).isEqualTo("owner");
	}

	@Test
	void getTeamDetailThrowsExceptionWhenTeamDoesNotExist() {
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(999L, TeamStatus.DELETED))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> teamService.getTeamDetail(999L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.TEAM_NOT_FOUND);
	}

	@Test
	void getTeamDetailThrowsExceptionWhenOwnerIsMissing() {
		User owner = activeUser();
		Team team = savedTeam(15L, owner, "GAME");
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(15L, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.findByTeamIdWithUser(15L)).thenReturn(List.of());

		assertThatThrownBy(() -> teamService.getTeamDetail(15L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.TEAM_OWNER_NOT_FOUND);
	}

	@Test
	void closeTeamSucceedsWhenRequesterIsOwner() {
		User owner = activeUser();
		Team team = savedTeam(16L, owner, "GAME");
		TeamMember ownerMember = team.getMembers().get(0);
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(16L, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.findByTeamIdAndRole(16L, TeamMemberRole.OWNER)).thenReturn(Optional.of(ownerMember));

		teamService.closeTeam(USER_ID, 16L);

		assertThat(team.getStatus()).isEqualTo(TeamStatus.CLOSED);
	}

	@Test
	void closeTeamThrowsExceptionWhenRequesterIsNotOwner() {
		User owner = activeUser();
		User notOwner = user(2L, "member@test.com", "member");
		Team team = savedTeam(17L, owner, "GAME");
		TeamMember ownerMember = team.getMembers().get(0);
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(17L, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.findByTeamIdAndRole(17L, TeamMemberRole.OWNER)).thenReturn(Optional.of(ownerMember));

		assertThatThrownBy(() -> teamService.closeTeam(notOwner.getId(), 17L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.TEAM_OWNER_REQUIRED);
	}

	@Test
	void closeTeamIsIdempotentWhenAlreadyClosed() {
		User owner = activeUser();
		Team team = savedTeam(18L, owner, "GAME");
		team.close();
		TeamMember ownerMember = team.getMembers().get(0);
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(18L, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.findByTeamIdAndRole(18L, TeamMemberRole.OWNER)).thenReturn(Optional.of(ownerMember));

		teamService.closeTeam(USER_ID, 18L);

		assertThat(team.getStatus()).isEqualTo(TeamStatus.CLOSED);
	}

	@Test
	void closeTeamThrowsExceptionWhenTeamDoesNotExist() {
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(999L, TeamStatus.DELETED))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> teamService.closeTeam(USER_ID, 999L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.TEAM_NOT_FOUND);
	}

	@Test
	void closeTeamThrowsExceptionWhenTeamIsDeleted() {
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(19L, TeamStatus.DELETED))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> teamService.closeTeam(USER_ID, 19L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.TEAM_NOT_FOUND);
	}

	@Test
	void closeTeamThrowsExceptionWhenOwnerIsNotActive() {
		User owner = activeUser();
		Team team = savedTeam(20L, owner, "GAME");
		TeamMember ownerMember = team.getMembers().get(0);
		ReflectionTestUtils.setField(ownerMember, "status", TeamMemberStatus.INACTIVE);
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(20L, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.findByTeamIdAndRole(20L, TeamMemberRole.OWNER)).thenReturn(Optional.of(ownerMember));

		assertThatThrownBy(() -> teamService.closeTeam(USER_ID, 20L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.TEAM_OWNER_REQUIRED);
	}

	@Test
	void deleteTeamSucceedsWhenRequesterIsOwner() {
		User owner = activeUser();
		Team team = savedTeam(21L, owner, "GAME");
		TeamMember ownerMember = team.getMembers().get(0);
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(21L, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.findByTeamIdAndRole(21L, TeamMemberRole.OWNER)).thenReturn(Optional.of(ownerMember));

		teamService.deleteTeam(USER_ID, 21L);

		assertThat(team.getDeletedAt()).isNotNull();
	}

	@Test
	void deleteTeamThrowsExceptionWhenRequesterIsNotOwner() {
		User owner = activeUser();
		User notOwner = user(2L, "member@test.com", "member");
		Team team = savedTeam(22L, owner, "GAME");
		TeamMember ownerMember = team.getMembers().get(0);
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(22L, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.findByTeamIdAndRole(22L, TeamMemberRole.OWNER)).thenReturn(Optional.of(ownerMember));

		assertThatThrownBy(() -> teamService.deleteTeam(notOwner.getId(), 22L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.TEAM_OWNER_REQUIRED);
	}

	@Test
	void deleteTeamThrowsExceptionWhenTeamDoesNotExist() {
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(999L, TeamStatus.DELETED))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> teamService.deleteTeam(USER_ID, 999L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.TEAM_NOT_FOUND);
	}

	@Test
	void deleteTeamThrowsExceptionWhenTeamIsDeleted() {
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(23L, TeamStatus.DELETED))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> teamService.deleteTeam(USER_ID, 23L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.TEAM_NOT_FOUND);
	}

	@Test
	void deleteTeamThrowsExceptionWhenOwnerIsNotActive() {
		User owner = activeUser();
		Team team = savedTeam(24L, owner, "GAME");
		TeamMember ownerMember = team.getMembers().get(0);
		ReflectionTestUtils.setField(ownerMember, "status", TeamMemberStatus.INACTIVE);
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(24L, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.findByTeamIdAndRole(24L, TeamMemberRole.OWNER)).thenReturn(Optional.of(ownerMember));

		assertThatThrownBy(() -> teamService.deleteTeam(USER_ID, 24L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.TEAM_OWNER_REQUIRED);
	}

	private TeamCreateRequest request() {
		return new TeamCreateRequest("Game Team", "Weekend game team", "GAME");
	}

	private Team savedTeam(Long teamId, User owner, String category) {
		Team team = Team.create("Game Team", "Weekend game team", category, owner);
		ReflectionTestUtils.setField(team, "id", teamId);
		return team;
	}

	private User activeUser() {
		return user(USER_ID, "owner@test.com", "owner");
	}

	private User user(Long id, String email, String nickname) {
		User user = User.create(email, "encoded-password", nickname);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
