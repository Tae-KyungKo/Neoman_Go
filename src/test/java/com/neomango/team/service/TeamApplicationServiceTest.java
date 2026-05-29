package com.neomango.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.team.dto.TeamApplicationCreateRequest;
import com.neomango.team.dto.TeamApplicationResponse;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamApplicationStatus;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamMemberRole;
import com.neomango.team.entity.TeamMemberStatus;
import com.neomango.team.entity.TeamStatus;
import com.neomango.team.exception.ApplicationAlreadyProcessedException;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
import com.neomango.team.repository.UserCategoryMembershipRepository;
import com.neomango.user.entity.User;
import com.neomango.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TeamApplicationServiceTest {

	private static final Long TEAM_ID = 10L;
	private static final Long APPLICANT_ID = 2L;
	private static final String MESSAGE = "가입하고 싶습니다.";

	@Mock
	private TeamApplicationRepository teamApplicationRepository;

	@Mock
	private TeamRepository teamRepository;

	@Mock
	private TeamMemberRepository teamMemberRepository;

	@Mock
	private UserCategoryMembershipRepository userCategoryMembershipRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private TeamApplicationService teamApplicationService;

	@Test
	void createApplicationSucceedsWhenTeamIsRecruiting() {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, user(1L, "owner@test.com", "owner"), "FUTSAL");
		when(userRepository.findById(APPLICANT_ID)).thenReturn(Optional.of(applicant));
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(TEAM_ID, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.existsByTeamIdAndUserIdAndStatus(TEAM_ID, APPLICANT_ID, TeamMemberStatus.ACTIVE))
			.thenReturn(false);
		when(teamMemberRepository.existsActiveMemberByUserIdAndTeamCategory(APPLICANT_ID, "FUTSAL"))
			.thenReturn(false);
		when(teamApplicationRepository.existsByTeamIdAndApplicantIdAndStatus(
			TEAM_ID,
			APPLICANT_ID,
			TeamApplicationStatus.PENDING
		)).thenReturn(false);
		when(teamApplicationRepository.save(any(TeamApplication.class))).thenAnswer(invocation -> {
			TeamApplication application = invocation.getArgument(0);
			ReflectionTestUtils.setField(application, "id", 100L);
			return application;
		});

		TeamApplicationResponse response = teamApplicationService.createApplication(
			TEAM_ID,
			APPLICANT_ID,
			new TeamApplicationCreateRequest(MESSAGE)
		);

		assertThat(response.applicationId()).isEqualTo(100L);
		assertThat(response.teamId()).isEqualTo(TEAM_ID);
		assertThat(response.teamName()).isEqualTo("Team");
		assertThat(response.status()).isEqualTo(TeamApplicationStatus.PENDING);
		assertThat(response.message()).isEqualTo(MESSAGE);

		ArgumentCaptor<TeamApplication> captor = ArgumentCaptor.forClass(TeamApplication.class);
		verify(teamApplicationRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(TeamApplicationStatus.PENDING);
	}

	@Test
	void createApplicationFailsWhenTeamDoesNotExist() {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		when(userRepository.findById(APPLICANT_ID)).thenReturn(Optional.of(applicant));
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(TEAM_ID, TeamStatus.DELETED))
			.thenReturn(Optional.empty());

		assertBusinessException(
			() -> teamApplicationService.createApplication(TEAM_ID, APPLICANT_ID, request()),
			ErrorCode.TEAM_NOT_FOUND
		);

		verifyNoInteractions(teamApplicationRepository);
	}

	@Test
	void createApplicationFailsWhenTeamStatusIsDeleted() {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, user(1L, "owner@test.com", "owner"), "FUTSAL");
		team.softDelete();
		when(userRepository.findById(APPLICANT_ID)).thenReturn(Optional.of(applicant));
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(TEAM_ID, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));

		assertBusinessException(
			() -> teamApplicationService.createApplication(TEAM_ID, APPLICANT_ID, request()),
			ErrorCode.TEAM_NOT_FOUND
		);
	}

	@Test
	void createApplicationFailsWhenTeamDeletedAtExists() {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, user(1L, "owner@test.com", "owner"), "FUTSAL");
		ReflectionTestUtils.setField(team, "deletedAt", LocalDateTime.now());
		when(userRepository.findById(APPLICANT_ID)).thenReturn(Optional.of(applicant));
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(TEAM_ID, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));

		assertBusinessException(
			() -> teamApplicationService.createApplication(TEAM_ID, APPLICANT_ID, request()),
			ErrorCode.TEAM_NOT_FOUND
		);
	}

	@Test
	void createApplicationFailsWhenTeamIsClosed() {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, user(1L, "owner@test.com", "owner"), "FUTSAL");
		team.close();
		when(userRepository.findById(APPLICANT_ID)).thenReturn(Optional.of(applicant));
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(TEAM_ID, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));

		assertBusinessException(
			() -> teamApplicationService.createApplication(TEAM_ID, APPLICANT_ID, request()),
			ErrorCode.TEAM_CLOSED
		);
	}

	@Test
	void createApplicationFailsWhenApplicantIsTeamMember() {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, user(1L, "owner@test.com", "owner"), "FUTSAL");
		when(userRepository.findById(APPLICANT_ID)).thenReturn(Optional.of(applicant));
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(TEAM_ID, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.existsByTeamIdAndUserIdAndStatus(TEAM_ID, APPLICANT_ID, TeamMemberStatus.ACTIVE))
			.thenReturn(true);

		assertBusinessException(
			() -> teamApplicationService.createApplication(TEAM_ID, APPLICANT_ID, request()),
			ErrorCode.ALREADY_TEAM_MEMBER
		);
	}

	@Test
	void createApplicationFailsWhenOwnerAppliesToOwnTeam() {
		User owner = user(APPLICANT_ID, "owner@test.com", "owner");
		Team team = team(TEAM_ID, owner, "FUTSAL");
		when(userRepository.findById(APPLICANT_ID)).thenReturn(Optional.of(owner));
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(TEAM_ID, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.existsByTeamIdAndUserIdAndStatus(TEAM_ID, APPLICANT_ID, TeamMemberStatus.ACTIVE))
			.thenReturn(true);

		assertBusinessException(
			() -> teamApplicationService.createApplication(TEAM_ID, APPLICANT_ID, request()),
			ErrorCode.ALREADY_TEAM_MEMBER
		);
	}

	@Test
	void createApplicationFailsWhenPendingApplicationExists() {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, user(1L, "owner@test.com", "owner"), "FUTSAL");
		when(userRepository.findById(APPLICANT_ID)).thenReturn(Optional.of(applicant));
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(TEAM_ID, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.existsByTeamIdAndUserIdAndStatus(TEAM_ID, APPLICANT_ID, TeamMemberStatus.ACTIVE))
			.thenReturn(false);
		when(teamMemberRepository.existsActiveMemberByUserIdAndTeamCategory(APPLICANT_ID, "FUTSAL"))
			.thenReturn(false);
		when(teamApplicationRepository.existsByTeamIdAndApplicantIdAndStatus(
			TEAM_ID,
			APPLICANT_ID,
			TeamApplicationStatus.PENDING
		)).thenReturn(true);

		assertBusinessException(
			() -> teamApplicationService.createApplication(TEAM_ID, APPLICANT_ID, request()),
			ErrorCode.DUPLICATE_PENDING_TEAM_APPLICATION
		);
	}

	@Test
	void createApplicationSucceedsWhenOnlyRejectedHistoryExists() {
		createApplicationSucceedsWhenNoPendingApplicationExists();
	}

	@Test
	void createApplicationSucceedsWhenOnlyCanceledHistoryExists() {
		createApplicationSucceedsWhenNoPendingApplicationExists();
	}

	@Test
	void createApplicationFailsWhenApplicantAlreadyBelongsToSameCategoryTeam() {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, user(1L, "owner@test.com", "owner"), "FUTSAL");
		when(userRepository.findById(APPLICANT_ID)).thenReturn(Optional.of(applicant));
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(TEAM_ID, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.existsByTeamIdAndUserIdAndStatus(TEAM_ID, APPLICANT_ID, TeamMemberStatus.ACTIVE))
			.thenReturn(false);
		when(teamMemberRepository.existsActiveMemberByUserIdAndTeamCategory(APPLICANT_ID, "FUTSAL"))
			.thenReturn(true);

		assertBusinessException(
			() -> teamApplicationService.createApplication(TEAM_ID, APPLICANT_ID, request()),
			ErrorCode.ALREADY_CATEGORY_TEAM_MEMBER
		);
	}

	@Test
	void cancelApplicationSucceedsWhenRequesterOwnsPendingApplication() {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, user(1L, "owner@test.com", "owner"), "FUTSAL");
		TeamApplication application = application(100L, team, applicant);
		when(teamApplicationRepository.findByIdWithTeam(100L)).thenReturn(Optional.of(application));

		TeamApplicationResponse response = teamApplicationService.cancelApplication(100L, APPLICANT_ID);

		assertThat(response.applicationId()).isEqualTo(100L);
		assertThat(response.teamId()).isEqualTo(TEAM_ID);
		assertThat(response.teamName()).isEqualTo("Team");
		assertThat(response.status()).isEqualTo(TeamApplicationStatus.CANCELED);
		assertThat(response.message()).isEqualTo(MESSAGE);
		assertThat(application.getStatus()).isEqualTo(TeamApplicationStatus.CANCELED);
	}

	@Test
	void cancelApplicationRecordsCanceledAt() {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, user(1L, "owner@test.com", "owner"), "FUTSAL");
		TeamApplication application = application(100L, team, applicant);
		when(teamApplicationRepository.findByIdWithTeam(100L)).thenReturn(Optional.of(application));

		teamApplicationService.cancelApplication(100L, APPLICANT_ID);

		assertThat(application.getCanceledAt()).isNotNull();
	}

	@Test
	void cancelApplicationFailsWhenApplicationDoesNotExist() {
		when(teamApplicationRepository.findByIdWithTeam(100L)).thenReturn(Optional.empty());

		assertBusinessException(
			() -> teamApplicationService.cancelApplication(100L, APPLICANT_ID),
			ErrorCode.TEAM_APPLICATION_NOT_FOUND
		);
	}

	@Test
	void cancelApplicationFailsWhenRequesterIsNotApplicant() {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, user(1L, "owner@test.com", "owner"), "FUTSAL");
		TeamApplication application = application(100L, team, applicant);
		when(teamApplicationRepository.findByIdWithTeam(100L)).thenReturn(Optional.of(application));

		assertBusinessException(
			() -> teamApplicationService.cancelApplication(100L, 999L),
			ErrorCode.TEAM_APPLICATION_CANCEL_FORBIDDEN
		);
	}

	@Test
	void cancelApplicationFailsWhenApplicationIsApproved() {
		assertCancelFailsWhenApplicationStatusIs(TeamApplicationStatus.APPROVED);
	}

	@Test
	void cancelApplicationFailsWhenApplicationIsRejected() {
		assertCancelFailsWhenApplicationStatusIs(TeamApplicationStatus.REJECTED);
	}

	@Test
	void cancelApplicationFailsWhenApplicationIsAlreadyCanceled() {
		assertCancelFailsWhenApplicationStatusIs(TeamApplicationStatus.CANCELED);
	}

	@Test
	void cancelApplicationSucceedsWhenTeamIsClosed() {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, user(1L, "owner@test.com", "owner"), "FUTSAL");
		team.close();
		TeamApplication application = application(100L, team, applicant);
		when(teamApplicationRepository.findByIdWithTeam(100L)).thenReturn(Optional.of(application));

		TeamApplicationResponse response = teamApplicationService.cancelApplication(100L, APPLICANT_ID);

		assertThat(response.status()).isEqualTo(TeamApplicationStatus.CANCELED);
	}

	@Test
	void cancelApplicationSucceedsWhenTeamIsDeleted() {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, user(1L, "owner@test.com", "owner"), "FUTSAL");
		team.softDelete();
		TeamApplication application = application(100L, team, applicant);
		when(teamApplicationRepository.findByIdWithTeam(100L)).thenReturn(Optional.of(application));

		TeamApplicationResponse response = teamApplicationService.cancelApplication(100L, APPLICANT_ID);

		assertThat(response.status()).isEqualTo(TeamApplicationStatus.CANCELED);
	}

	@Test
	void approveApplicationCreatesTeamMemberAndApprovesPendingApplication() {
		User owner = user(1L, "owner@test.com", "owner");
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, owner, "FUTSAL");
		TeamApplication application = application(100L, team, applicant);
		when(teamApplicationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(application));
		when(teamMemberRepository.findByTeamIdAndRole(TEAM_ID, TeamMemberRole.OWNER))
			.thenReturn(Optional.of(team.getMembers().get(0)));
		when(teamMemberRepository.existsByTeamIdAndUserIdAndStatus(TEAM_ID, APPLICANT_ID, TeamMemberStatus.ACTIVE))
			.thenReturn(false);
		when(teamMemberRepository.existsActiveMemberByUserIdAndTeamCategory(APPLICANT_ID, "FUTSAL"))
			.thenReturn(false);
		when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, APPLICANT_ID)).thenReturn(Optional.empty());
		when(teamApplicationRepository.findOtherPendingApplicationsInSameCategory(100L, APPLICANT_ID, "FUTSAL"))
			.thenReturn(List.of());

		TeamApplicationResponse response = teamApplicationService.approveApplication(100L, owner.getId());

		assertThat(response.status()).isEqualTo(TeamApplicationStatus.APPROVED);
		assertThat(application.getStatus()).isEqualTo(TeamApplicationStatus.APPROVED);
		ArgumentCaptor<TeamMember> captor = ArgumentCaptor.forClass(TeamMember.class);
		verify(teamMemberRepository).save(captor.capture());
		assertThat(captor.getValue().getTeam()).isSameAs(team);
		assertThat(captor.getValue().getUser()).isSameAs(applicant);
		assertThat(captor.getValue().getRole()).isEqualTo(TeamMemberRole.MEMBER);
	}

	@Test
	void rejectApplicationRejectsPendingApplicationWithoutCreatingTeamMember() {
		User owner = user(1L, "owner@test.com", "owner");
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, owner, "FUTSAL");
		TeamApplication application = application(100L, team, applicant);
		when(teamApplicationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(application));
		when(teamMemberRepository.findByTeamIdAndRole(TEAM_ID, TeamMemberRole.OWNER))
			.thenReturn(Optional.of(team.getMembers().get(0)));

		TeamApplicationResponse response = teamApplicationService.rejectApplication(100L, owner.getId());

		assertThat(response.status()).isEqualTo(TeamApplicationStatus.REJECTED);
		assertThat(application.getStatus()).isEqualTo(TeamApplicationStatus.REJECTED);
		verify(teamMemberRepository, never()).save(any(TeamMember.class));
	}

	@Test
	void approveAndRejectApplicationFailWhenRequesterIsNotOwner() {
		User owner = user(1L, "owner@test.com", "owner");
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, owner, "FUTSAL");
		when(teamApplicationRepository.findByIdForUpdate(100L))
			.thenReturn(Optional.of(application(100L, team, applicant)))
			.thenReturn(Optional.of(application(101L, team, applicant)));
		when(teamMemberRepository.findByTeamIdAndRole(TEAM_ID, TeamMemberRole.OWNER))
			.thenReturn(Optional.of(team.getMembers().get(0)));

		assertBusinessException(
			() -> teamApplicationService.approveApplication(100L, 999L),
			ErrorCode.TEAM_OWNER_REQUIRED
		);
		assertBusinessException(
			() -> teamApplicationService.rejectApplication(100L, 999L),
			ErrorCode.TEAM_OWNER_REQUIRED
		);

		verify(teamMemberRepository, never()).save(any(TeamMember.class));
	}

	@Test
	void approveAndRejectApplicationFailWhenApplicationIsAlreadyProcessed() {
		assertProcessFailsWhenApplicationStatusIs(TeamApplicationStatus.APPROVED);
		assertProcessFailsWhenApplicationStatusIs(TeamApplicationStatus.REJECTED);
		assertProcessFailsWhenApplicationStatusIs(TeamApplicationStatus.CANCELED);
	}

	@Test
	void approveApplicationCancelsOtherPendingApplicationsInSameCategory() {
		User owner = user(1L, "owner@test.com", "owner");
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, owner, "FUTSAL");
		TeamApplication application = application(100L, team, applicant);
		TeamApplication otherApplication = application(101L, team(11L, user(3L, "other-owner@test.com", "other"), "FUTSAL"), applicant);
		when(teamApplicationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(application));
		when(teamMemberRepository.findByTeamIdAndRole(TEAM_ID, TeamMemberRole.OWNER))
			.thenReturn(Optional.of(team.getMembers().get(0)));
		when(teamMemberRepository.existsByTeamIdAndUserIdAndStatus(TEAM_ID, APPLICANT_ID, TeamMemberStatus.ACTIVE))
			.thenReturn(false);
		when(teamMemberRepository.existsActiveMemberByUserIdAndTeamCategory(APPLICANT_ID, "FUTSAL"))
			.thenReturn(false);
		when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, APPLICANT_ID)).thenReturn(Optional.empty());
		when(teamApplicationRepository.findOtherPendingApplicationsInSameCategory(100L, APPLICANT_ID, "FUTSAL"))
			.thenReturn(List.of(otherApplication));

		teamApplicationService.approveApplication(100L, owner.getId());

		assertThat(application.getStatus()).isEqualTo(TeamApplicationStatus.APPROVED);
		assertThat(otherApplication.getStatus()).isEqualTo(TeamApplicationStatus.CANCELED);
	}

	private void createApplicationSucceedsWhenNoPendingApplicationExists() {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, user(1L, "owner@test.com", "owner"), "FUTSAL");
		when(userRepository.findById(APPLICANT_ID)).thenReturn(Optional.of(applicant));
		when(teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(TEAM_ID, TeamStatus.DELETED))
			.thenReturn(Optional.of(team));
		when(teamMemberRepository.existsByTeamIdAndUserIdAndStatus(TEAM_ID, APPLICANT_ID, TeamMemberStatus.ACTIVE))
			.thenReturn(false);
		when(teamMemberRepository.existsActiveMemberByUserIdAndTeamCategory(APPLICANT_ID, "FUTSAL"))
			.thenReturn(false);
		when(teamApplicationRepository.existsByTeamIdAndApplicantIdAndStatus(
			TEAM_ID,
			APPLICANT_ID,
			TeamApplicationStatus.PENDING
		)).thenReturn(false);
		when(teamApplicationRepository.save(any(TeamApplication.class))).thenAnswer(invocation -> {
			TeamApplication application = invocation.getArgument(0);
			ReflectionTestUtils.setField(application, "id", 101L);
			return application;
		});

		TeamApplicationResponse response = teamApplicationService.createApplication(
			TEAM_ID,
			APPLICANT_ID,
			request()
		);

		assertThat(response.status()).isEqualTo(TeamApplicationStatus.PENDING);
	}

	private void assertBusinessException(Runnable runnable, ErrorCode errorCode) {
		assertThatThrownBy(runnable::run)
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(errorCode);
	}

	private void assertCancelFailsWhenApplicationStatusIs(TeamApplicationStatus status) {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, user(1L, "owner@test.com", "owner"), "FUTSAL");
		TeamApplication application = application(100L, team, applicant);
		switch (status) {
			case APPROVED -> application.approve();
			case REJECTED -> application.reject();
			case CANCELED -> application.cancel();
			default -> throw new IllegalArgumentException("Unsupported status: " + status);
		}
		when(teamApplicationRepository.findByIdWithTeam(100L)).thenReturn(Optional.of(application));

		assertBusinessException(
			() -> teamApplicationService.cancelApplication(100L, APPLICANT_ID),
			ErrorCode.ONLY_PENDING_TEAM_APPLICATION_CANCELABLE
		);
	}

	private void assertProcessFailsWhenApplicationStatusIs(TeamApplicationStatus status) {
		User applicant = user(APPLICANT_ID, "applicant@test.com", "applicant");
		Team team = team(TEAM_ID, user(1L, "owner@test.com", "owner"), "FUTSAL");
		TeamApplication application = application(100L, team, applicant);
		switch (status) {
			case APPROVED -> application.approve();
			case REJECTED -> application.reject();
			case CANCELED -> application.cancel();
			default -> throw new IllegalArgumentException("Unsupported status: " + status);
		}
		when(teamApplicationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(application));

		assertThatThrownBy(() -> teamApplicationService.approveApplication(100L, 1L))
			.isInstanceOf(ApplicationAlreadyProcessedException.class);
		when(teamApplicationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(application));
		assertThatThrownBy(() -> teamApplicationService.rejectApplication(100L, 1L))
			.isInstanceOf(ApplicationAlreadyProcessedException.class);
	}

	private TeamApplicationCreateRequest request() {
		return new TeamApplicationCreateRequest(MESSAGE);
	}

	private Team team(Long teamId, User owner, String category) {
		Team team = Team.create("Team", null, category, owner);
		ReflectionTestUtils.setField(team, "id", teamId);
		return team;
	}

	private TeamApplication application(Long applicationId, Team team, User applicant) {
		TeamApplication application = TeamApplication.create(team, applicant, MESSAGE);
		ReflectionTestUtils.setField(application, "id", applicationId);
		return application;
	}

	private User user(Long userId, String email, String nickname) {
		User user = User.create(email, "encoded-password", nickname);
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}
}
