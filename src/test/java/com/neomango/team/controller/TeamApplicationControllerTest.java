package com.neomango.team.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.neomango.auth.jwt.JwtTokenProvider;
import com.neomango.notification.entity.Notification;
import com.neomango.notification.entity.NotificationTargetType;
import com.neomango.notification.entity.NotificationType;
import com.neomango.notification.repository.NotificationRepository;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamApplicationStatus;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.UserCategoryMembership;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamRepository;
import com.neomango.team.repository.UserCategoryMembershipRepository;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;
import com.neomango.user.repository.UserRepository;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class TeamApplicationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private TeamMemberRepository teamMemberRepository;

	@Autowired
	private UserCategoryMembershipRepository userCategoryMembershipRepository;

	@Autowired
	private TeamApplicationRepository teamApplicationRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@BeforeEach
	void setUp() {
		cleanUp();
	}

	@AfterEach
	void tearDown() {
		cleanUp();
	}

	private void cleanUp() {
		notificationRepository.deleteAll();
		teamApplicationRepository.deleteAll();
		userCategoryMembershipRepository.deleteAll();
		teamMemberRepository.deleteAll();
		teamRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void cancelTeamApplicationReturnsCanceledApplicationWhenAuthenticatedApplicantRequests() throws Exception {
		User owner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "owner@test.com", "encoded-password", "owner"));
		User applicant = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "applicant@test.com", "encoded-password", "applicant"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		TeamApplication application = teamApplicationRepository.save(
			TeamApplication.create(team, applicant, "가입하고 싶습니다.")
		);
		String accessToken = jwtTokenProvider.createAccessToken(applicant.getId(), UserRole.USER);

		mockMvc.perform(patch("/api/team-applications/{applicationId}/cancel", application.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.applicationId").value(application.getId()))
			.andExpect(jsonPath("$.data.teamId").value(team.getId()))
			.andExpect(jsonPath("$.data.teamName").value("Futsal Team"))
			.andExpect(jsonPath("$.data.status").value("CANCELED"))
			.andExpect(jsonPath("$.data.message").value("가입하고 싶습니다."));

		TeamApplication canceledApplication = teamApplicationRepository.findById(application.getId()).orElseThrow();
		assertThat(canceledApplication.getStatus()).isEqualTo(TeamApplicationStatus.CANCELED);
		assertThat(canceledApplication.getCanceledAt()).isNotNull();
	}

	@Test
	void cancelTeamApplicationRejectsRequestWithoutAuthentication() throws Exception {
		mockMvc.perform(patch("/api/team-applications/{applicationId}/cancel", 1L))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void approveApplication_returnsApprovedStatusWhenRequesterIsOwner() throws Exception {
		User owner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "owner@test.com", "encoded-password", "owner"));
		User applicant = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "applicant@test.com", "encoded-password", "applicant"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		TeamApplication application = teamApplicationRepository.save(
			TeamApplication.create(team, applicant, "가입하고 싶습니다.")
		);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/team-applications/{applicationId}/approve", application.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.applicationId").value(application.getId()))
			.andExpect(jsonPath("$.data.teamId").value(team.getId()))
			.andExpect(jsonPath("$.data.teamName").value("Futsal Team"))
			.andExpect(jsonPath("$.data.status").value("APPROVED"))
			.andExpect(jsonPath("$.data.message").value("가입하고 싶습니다."));

		TeamApplication approvedApplication = teamApplicationRepository.findById(application.getId()).orElseThrow();
		assertThat(approvedApplication.getStatus()).isEqualTo(TeamApplicationStatus.APPROVED);
		assertThat(teamMemberRepository.existsByTeamIdAndUserId(team.getId(), applicant.getId())).isTrue();
		assertThat(notificationRepository.count()).isEqualTo(1);
		Notification notification = notificationRepository.findAll().get(0);
		assertThat(notification.getReceiver().getId()).isEqualTo(applicant.getId());
		assertThat(notification.getType()).isEqualTo(NotificationType.TEAM_APPLICATION_APPROVED);
		assertThat(notification.getTitle()).isEqualTo("가입 신청 승인");
		assertThat(notification.getMessage()).isEqualTo("Futsal Team 팀 가입 신청이 승인되었습니다.");
		assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.TEAM_APPLICATION);
		assertThat(notification.getTargetId()).isEqualTo(application.getId());
	}

	@Test
	void approveApplication_createsJoinedNotificationForExistingActiveMemberOnly() throws Exception {
		User owner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "owner-joined@test.com", "encoded-password", "owner"));
		User member = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "member-joined@test.com", "encoded-password", "member"));
		User inactiveUser = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "inactive-joined@test.com", "encoded-password", "inactive"));
		User applicant = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "applicant-joined@test.com", "encoded-password", "applicant"));
		Team team = Team.create("Futsal Team", null, "FUTSAL", owner);
		team.addMember(TeamMember.createMember(team, member));
		TeamMember inactiveMember = TeamMember.createMember(team, inactiveUser);
		inactiveMember.deactivate();
		team.addMember(inactiveMember);
		Team savedTeam = teamRepository.saveAndFlush(team);
		TeamApplication application = teamApplicationRepository.save(
			TeamApplication.create(savedTeam, applicant, "가입하고 싶습니다.")
		);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/team-applications/{applicationId}/approve", application.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("APPROVED"));

		List<Notification> notifications = notificationRepository.findAll();
		assertThat(notifications).hasSize(2);
		Notification approvedNotification = notifications.stream()
			.filter(notification -> notification.getType() == NotificationType.TEAM_APPLICATION_APPROVED)
			.findFirst()
			.orElseThrow();
		Notification joinedNotification = notifications.stream()
			.filter(notification -> notification.getType() == NotificationType.TEAM_MEMBER_JOINED)
			.findFirst()
			.orElseThrow();
		assertThat(approvedNotification.getReceiver().getId()).isEqualTo(applicant.getId());
		assertThat(joinedNotification.getReceiver().getId()).isEqualTo(member.getId());
		assertThat(joinedNotification.getReceiver().getId()).isNotEqualTo(owner.getId());
		assertThat(joinedNotification.getReceiver().getId()).isNotEqualTo(applicant.getId());
		assertThat(joinedNotification.getReceiver().getId()).isNotEqualTo(inactiveUser.getId());
		assertThat(joinedNotification.getType()).isEqualTo(NotificationType.TEAM_MEMBER_JOINED);
		assertThat(joinedNotification.getTitle()).isEqualTo("새 멤버 가입");
		assertThat(joinedNotification.getMessage()).isEqualTo("applicant님이 Futsal Team 팀에 합류했습니다.");
		assertThat(joinedNotification.getTargetType()).isEqualTo(NotificationTargetType.TEAM);
		assertThat(joinedNotification.getTargetId()).isEqualTo(savedTeam.getId());
	}

	@Test
	void rejectApplication_returnsRejectedStatusWhenRequesterIsOwner() throws Exception {
		User owner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "owner@test.com", "encoded-password", "owner"));
		User applicant = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "applicant@test.com", "encoded-password", "applicant"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		TeamApplication application = teamApplicationRepository.save(
			TeamApplication.create(team, applicant, "가입하고 싶습니다.")
		);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/team-applications/{applicationId}/reject", application.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.applicationId").value(application.getId()))
			.andExpect(jsonPath("$.data.status").value("REJECTED"));

		TeamApplication rejectedApplication = teamApplicationRepository.findById(application.getId()).orElseThrow();
		assertThat(rejectedApplication.getStatus()).isEqualTo(TeamApplicationStatus.REJECTED);
		assertThat(teamMemberRepository.existsByTeamIdAndUserId(team.getId(), applicant.getId())).isFalse();
		Notification notification = notificationRepository.findAll().get(0);
		assertThat(notification.getReceiver().getId()).isEqualTo(applicant.getId());
		assertThat(notification.getType()).isEqualTo(NotificationType.TEAM_APPLICATION_REJECTED);
		assertThat(notification.getTitle()).isEqualTo("가입 신청 거절");
		assertThat(notification.getMessage()).isEqualTo("Futsal Team 팀 가입 신청이 거절되었습니다.");
		assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.TEAM_APPLICATION);
		assertThat(notification.getTargetId()).isEqualTo(application.getId());
	}

	@Test
	void approveApplication_failsWhenRequesterIsNotOwner() throws Exception {
		User owner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "owner@test.com", "encoded-password", "owner"));
		User applicant = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "applicant@test.com", "encoded-password", "applicant"));
		User outsider = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "outsider@test.com", "encoded-password", "outsider"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		TeamApplication application = teamApplicationRepository.save(
			TeamApplication.create(team, applicant, "가입하고 싶습니다.")
		);
		String accessToken = jwtTokenProvider.createAccessToken(outsider.getId(), UserRole.USER);

		mockMvc.perform(post("/api/team-applications/{applicationId}/approve", application.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("T002"));
		assertThat(notificationRepository.count()).isZero();
	}

	@Test
	void rejectApplication_failsWhenRequesterIsNotOwner() throws Exception {
		User owner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "owner@test.com", "encoded-password", "owner"));
		User applicant = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "applicant@test.com", "encoded-password", "applicant"));
		User outsider = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "outsider@test.com", "encoded-password", "outsider"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		TeamApplication application = teamApplicationRepository.save(
			TeamApplication.create(team, applicant, "가입하고 싶습니다.")
		);
		String accessToken = jwtTokenProvider.createAccessToken(outsider.getId(), UserRole.USER);

		mockMvc.perform(post("/api/team-applications/{applicationId}/reject", application.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("T002"));
		assertThat(notificationRepository.count()).isZero();
	}

	@Test
	void approveApplication_failsWhenApplicationAlreadyProcessed() throws Exception {
		User owner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "owner@test.com", "encoded-password", "owner"));
		User applicant = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "applicant@test.com", "encoded-password", "applicant"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		TeamApplication application = TeamApplication.create(team, applicant, "가입하고 싶습니다.");
		application.approve();
		TeamApplication savedApplication = teamApplicationRepository.save(application);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/team-applications/{applicationId}/approve", savedApplication.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("TA003"));
		assertThat(notificationRepository.count()).isZero();
	}

	@Test
	void approveApplication_failsWhenApplicationNotFound() throws Exception {
		User owner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "owner@test.com", "encoded-password", "owner"));
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/team-applications/{applicationId}/approve", 999L)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TA001"));
	}

	@Test
	void approveApplication_failsWhenApplicantAlreadyBelongsToSameCategory() throws Exception {
		User owner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "owner@test.com", "encoded-password", "owner"));
		User existingOwner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "existing-owner@test.com", "encoded-password", "existingOwner"));
		User applicant = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "applicant@test.com", "encoded-password", "applicant"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		Team existingTeam = teamRepository.save(Team.create("Existing Futsal Team", null, "FUTSAL", existingOwner));
		userCategoryMembershipRepository.saveAndFlush(
			UserCategoryMembership.create(applicant, "FUTSAL", existingTeam)
		);
		TeamApplication application = teamApplicationRepository.save(
			TeamApplication.create(team, applicant, "message")
		);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/team-applications/{applicationId}/approve", application.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("T007"));
	}

	@Test
	void approveApplication_failsWhenApplicantIsActiveTeamMember() throws Exception {
		User owner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "owner@test.com", "encoded-password", "owner"));
		User applicant = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "applicant@test.com", "encoded-password", "applicant"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		teamMemberRepository.saveAndFlush(TeamMember.createMember(team, applicant));
		TeamApplication application = teamApplicationRepository.save(
			TeamApplication.create(team, applicant, "message")
		);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/team-applications/{applicationId}/approve", application.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("T003"));
	}

	@Test
	void approveApplication_cancelsOtherPendingApplicationsInSameCategory() throws Exception {
		User owner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "owner@test.com", "encoded-password", "owner"));
		User otherOwner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "other-owner@test.com", "encoded-password", "otherOwner"));
		User applicant = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "applicant@test.com", "encoded-password", "applicant"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		Team otherSameCategoryTeam = teamRepository.save(Team.create("Other Futsal Team", null, "FUTSAL", otherOwner));
		TeamApplication application = teamApplicationRepository.save(
			TeamApplication.create(team, applicant, "first")
		);
		TeamApplication otherPendingApplication = teamApplicationRepository.save(
			TeamApplication.create(otherSameCategoryTeam, applicant, "second")
		);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/team-applications/{applicationId}/approve", application.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("APPROVED"));

		TeamApplication approvedApplication = teamApplicationRepository.findById(application.getId()).orElseThrow();
		TeamApplication canceledApplication = teamApplicationRepository.findById(otherPendingApplication.getId()).orElseThrow();
		assertThat(approvedApplication.getStatus()).isEqualTo(TeamApplicationStatus.APPROVED);
		assertThat(canceledApplication.getStatus()).isEqualTo(TeamApplicationStatus.CANCELED);
		assertThat(canceledApplication.isActive()).isFalse();
		assertThat(notificationRepository.findAll())
			.extracting(Notification::getType)
			.containsExactly(NotificationType.TEAM_APPLICATION_APPROVED);
	}

	@Test
	void approveApplication_doesNotCancelPendingApplicationsInDifferentCategory() throws Exception {
		User owner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "owner@test.com", "encoded-password", "owner"));
		User otherOwner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "other-owner@test.com", "encoded-password", "otherOwner"));
		User applicant = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "applicant@test.com", "encoded-password", "applicant"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		Team differentCategoryTeam = teamRepository.save(Team.create("Baseball Team", null, "BASEBALL", otherOwner));
		TeamApplication application = teamApplicationRepository.save(
			TeamApplication.create(team, applicant, "first")
		);
		TeamApplication differentCategoryApplication = teamApplicationRepository.save(
			TeamApplication.create(differentCategoryTeam, applicant, "baseball")
		);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/team-applications/{applicationId}/approve", application.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("APPROVED"));

		TeamApplication pendingApplication = teamApplicationRepository.findById(differentCategoryApplication.getId()).orElseThrow();
		assertThat(pendingApplication.getStatus()).isEqualTo(TeamApplicationStatus.PENDING);
		assertThat(pendingApplication.isActive()).isTrue();
	}

	@Test
	void rejectApplication_failsWhenApplicationAlreadyApproved() throws Exception {
		assertRejectFailsWhenApplicationAlreadyProcessed(TeamApplicationStatus.APPROVED);
	}

	@Test
	void rejectApplication_failsWhenApplicationAlreadyRejected() throws Exception {
		assertRejectFailsWhenApplicationAlreadyProcessed(TeamApplicationStatus.REJECTED);
	}

	@Test
	void rejectApplication_failsWhenApplicationAlreadyCanceled() throws Exception {
		assertRejectFailsWhenApplicationAlreadyProcessed(TeamApplicationStatus.CANCELED);
	}

	@Test
	void rejectApplication_failsWhenApplicationNotFound() throws Exception {
		User owner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "owner@test.com", "encoded-password", "owner"));
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/team-applications/{applicationId}/reject", 999L)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TA001"));
	}

	private void assertRejectFailsWhenApplicationAlreadyProcessed(TeamApplicationStatus status) throws Exception {
		User owner = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "owner-" + status + "@test.com", "encoded-password", "owner"));
		User applicant = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "applicant-" + status + "@test.com", "encoded-password", "applicant"));
		Team team = teamRepository.save(Team.create("Futsal Team " + status, null, "FUTSAL", owner));
		TeamApplication application = TeamApplication.create(team, applicant, "message");
		switch (status) {
			case APPROVED -> application.approve();
			case REJECTED -> application.reject();
			case CANCELED -> application.cancel();
			default -> throw new IllegalArgumentException("Unsupported status: " + status);
		}
		TeamApplication savedApplication = teamApplicationRepository.save(application);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/team-applications/{applicationId}/reject", savedApplication.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("TA003"));
		assertThat(notificationRepository.count()).isZero();
	}
}
