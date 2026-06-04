package com.neomango.team.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neomango.auth.jwt.JwtTokenProvider;
import com.neomango.notification.entity.Notification;
import com.neomango.notification.entity.NotificationTargetType;
import com.neomango.notification.entity.NotificationType;
import com.neomango.notification.repository.NotificationRepository;
import com.neomango.team.dto.OwnerDelegationRequest;
import com.neomango.team.dto.TeamApplicationCreateRequest;
import com.neomango.team.dto.TeamCreateRequest;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamApplicationStatus;
import com.neomango.team.entity.TeamMemberStatus;
import com.neomango.team.entity.TeamStatus;
import com.neomango.team.entity.UserCategoryMembership;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
import com.neomango.team.repository.UserCategoryMembershipRepository;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;
import com.neomango.user.repository.UserRepository;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class TeamControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private TeamMemberRepository teamMemberRepository;

	@Autowired
	private TeamApplicationRepository teamApplicationRepository;

	@Autowired
	private UserCategoryMembershipRepository userCategoryMembershipRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@BeforeEach
	void setUp() {
		notificationRepository.deleteAll();
		teamApplicationRepository.deleteAll();
		userCategoryMembershipRepository.deleteAll();
		teamMemberRepository.deleteAll();
		teamRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void createTeamReturnsCreatedWhenAuthenticated() throws Exception {
		User user = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), UserRole.USER);
		TeamCreateRequest request = new TeamCreateRequest("Game Team", "Weekend game team", "GAME");

		mockMvc.perform(post("/api/teams")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.name").value("Game Team"))
			.andExpect(jsonPath("$.data.description").value("Weekend game team"))
			.andExpect(jsonPath("$.data.category").value("GAME"))
			.andExpect(jsonPath("$.data.status").value("RECRUITING"))
			.andExpect(jsonPath("$.data.ownerId").value(user.getId()))
			.andExpect(jsonPath("$.data.ownerNickname").value("owner"));
	}

	@Test
	void createTeamRejectsRequestWithoutAuthentication() throws Exception {
		TeamCreateRequest request = new TeamCreateRequest("Game Team", "Weekend game team", "GAME");

		mockMvc.perform(post("/api/teams")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void createTeamReturnsBadRequestWhenNameIsBlank() throws Exception {
		User user = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), UserRole.USER);
		TeamCreateRequest request = new TeamCreateRequest(" ", "Weekend game team", "GAME");

		mockMvc.perform(post("/api/teams")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));
	}

	@Test
	void getTeamsReturnsTeamSummariesWithoutAuthentication() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		teamRepository.save(Team.create("Game Team", "Weekend game team", "GAME", owner));

		mockMvc.perform(get("/api/teams")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content[0].name").value("Game Team"))
			.andExpect(jsonPath("$.data.content[0].category").value("GAME"))
			.andExpect(jsonPath("$.data.content[0].status").value("RECRUITING"))
			.andExpect(jsonPath("$.data.content[0].ownerId").value(owner.getId()))
			.andExpect(jsonPath("$.data.content[0].ownerNickname").value("owner"));
	}

	@Test
	void getTeamsFiltersByCategoryWithoutAuthentication() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		teamRepository.save(Team.create("Game Team", null, "GAME", owner));
		teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));

		mockMvc.perform(get("/api/teams")
				.param("category", "FUTSAL")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.content[0].name").value("Futsal Team"))
			.andExpect(jsonPath("$.data.content[0].category").value("FUTSAL"));
	}

	@Test
	void getTeamsUsesDefaultSortByCreatedAtDescAndIdDesc() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		Team olderTeam = teamRepository.save(Team.create("Older Team", null, "GAME", owner));
		Team newerTeam = teamRepository.save(Team.create("Newer Team", null, "GAME", owner));
		ReflectionTestUtils.setField(olderTeam, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 0));
		ReflectionTestUtils.setField(newerTeam, "createdAt", LocalDateTime.of(2026, 1, 2, 0, 0));
		teamRepository.saveAndFlush(olderTeam);
		teamRepository.saveAndFlush(newerTeam);

		mockMvc.perform(get("/api/teams")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].name").value("Newer Team"))
			.andExpect(jsonPath("$.data.content[1].name").value("Older Team"));
	}

	@Test
	void getTeamsExcludesDeletedTeam() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		teamRepository.save(Team.create("Active Team", null, "GAME", owner));
		Team deletedTeam = Team.create("Deleted Team", null, "GAME", owner);
		ReflectionTestUtils.setField(deletedTeam, "deletedAt", LocalDateTime.now());
		teamRepository.saveAndFlush(deletedTeam);

		mockMvc.perform(get("/api/teams")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.content[0].name").value("Active Team"));
	}

	@Test
	void getTeamDetailReturnsMembersWithoutAuthentication() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		Team team = teamRepository.save(Team.create("Game Team", "Weekend game team", "GAME", owner));

		mockMvc.perform(get("/api/teams/{teamId}", team.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(team.getId()))
			.andExpect(jsonPath("$.data.name").value("Game Team"))
			.andExpect(jsonPath("$.data.description").value("Weekend game team"))
			.andExpect(jsonPath("$.data.category").value("GAME"))
			.andExpect(jsonPath("$.data.status").value("RECRUITING"))
			.andExpect(jsonPath("$.data.owner.userId").value(owner.getId()))
			.andExpect(jsonPath("$.data.owner.nickname").value("owner"))
			.andExpect(jsonPath("$.data.owner.role").value("OWNER"))
			.andExpect(jsonPath("$.data.members.length()").value(1))
			.andExpect(jsonPath("$.data.members[0].userId").value(owner.getId()))
			.andExpect(jsonPath("$.data.members[0].nickname").value("owner"))
			.andExpect(jsonPath("$.data.members[0].role").value("OWNER"))
			.andExpect(jsonPath("$.data.members[0].status").value("ACTIVE"));
	}

	@Test
	void getTeamMembersReturnsActiveMembersWhenAuthenticated() throws Exception {
		User owner = userRepository.save(User.create("owner-members@test.com", "encoded-password", "owner"));
		User member = userRepository.save(User.create("member-members@test.com", "encoded-password", "member"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, member));
		Team savedTeam = teamRepository.saveAndFlush(team);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);
		TeamMember ownerMember = savedTeam.getMembers().stream()
			.filter(TeamMember::isOwner)
			.findFirst()
			.orElseThrow();
		TeamMember normalMember = savedTeam.getMembers().stream()
			.filter(teamMember -> !teamMember.isOwner())
			.findFirst()
			.orElseThrow();

		mockMvc.perform(get("/api/teams/{teamId}/members", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].teamMemberId").value(ownerMember.getId()))
			.andExpect(jsonPath("$.data[0].userId").value(owner.getId()))
			.andExpect(jsonPath("$.data[0].nickname").value("owner"))
			.andExpect(jsonPath("$.data[0].role").value("OWNER"))
			.andExpect(jsonPath("$.data[0].status").value(TeamMemberStatus.ACTIVE.name()))
			.andExpect(jsonPath("$.data[0].joinedAt").doesNotExist())
			.andExpect(jsonPath("$.data[1].teamMemberId").value(normalMember.getId()))
			.andExpect(jsonPath("$.data[1].userId").value(member.getId()))
			.andExpect(jsonPath("$.data[1].nickname").value("member"))
			.andExpect(jsonPath("$.data[1].role").value("MEMBER"))
			.andExpect(jsonPath("$.data[1].status").value(TeamMemberStatus.ACTIVE.name()));
	}

	@Test
	void getTeamMembersExcludesInactiveMembers() throws Exception {
		User owner = userRepository.save(User.create("owner-active-members@test.com", "encoded-password", "owner"));
		User activeUser = userRepository.save(User.create("active-member-list@test.com", "encoded-password", "activeMember"));
		User inactiveUser = userRepository.save(User.create("inactive-member-list@test.com", "encoded-password", "inactiveMember"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, activeUser));
		TeamMember inactiveMember = TeamMember.createMember(team, inactiveUser);
		inactiveMember.deactivate();
		team.addMember(inactiveMember);
		Team savedTeam = teamRepository.saveAndFlush(team);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(get("/api/teams/{teamId}/members", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].nickname").value("owner"))
			.andExpect(jsonPath("$.data[1].nickname").value("activeMember"));
	}

	@Test
	void getTeamMembersReturnsNotFoundWhenTeamIsDeleted() throws Exception {
		User owner = userRepository.save(User.create("owner-deleted-members@test.com", "encoded-password", "owner"));
		Team team = Team.create("Deleted Team", null, "GAME", owner);
		team.softDelete();
		Team savedTeam = teamRepository.saveAndFlush(team);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(get("/api/teams/{teamId}/members", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T001"));
	}

	@Test
	void getTeamMembersReturnsNotFoundWhenTeamDoesNotExist() throws Exception {
		User user = userRepository.save(User.create("member-not-found-team@test.com", "encoded-password", "member"));
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), UserRole.USER);

		mockMvc.perform(get("/api/teams/{teamId}/members", 999L)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T001"));
	}

	@Test
	void getTeamMembersRejectsRequestWithoutAuthentication() throws Exception {
		User owner = userRepository.save(User.create("owner-members-auth@test.com", "encoded-password", "owner"));
		Team team = teamRepository.save(Team.create("Game Team", null, "GAME", owner));

		mockMvc.perform(get("/api/teams/{teamId}/members", team.getId()))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void leaveTeamSucceedsForNormalMember() throws Exception {
		User owner = userRepository.save(User.create("owner-leave@test.com", "encoded-password", "owner"));
		User member = userRepository.save(User.create("member-leave@test.com", "encoded-password", "member"));
		User otherOwner = userRepository.save(User.create("other-owner-leave@test.com", "encoded-password", "otherOwner"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, member));
		Team savedTeam = teamRepository.saveAndFlush(team);
		Team sportsTeam = teamRepository.saveAndFlush(Team.create("Sports Team", null, "SPORTS", otherOwner));
		userCategoryMembershipRepository.saveAndFlush(UserCategoryMembership.create(member, "GAME", savedTeam));
		userCategoryMembershipRepository.saveAndFlush(UserCategoryMembership.create(member, "SPORTS", sportsTeam));
		TeamMember memberTeamMember = savedTeam.getMembers().stream()
			.filter(teamMember -> teamMember.getUser().getId().equals(member.getId()))
			.findFirst()
			.orElseThrow();
		String memberToken = jwtTokenProvider.createAccessToken(member.getId(), UserRole.USER);
		String ownerToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/me/leave", savedTeam.getId())
				.header("Authorization", "Bearer " + memberToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").doesNotExist());

		TeamMember leftMember = teamMemberRepository.findById(memberTeamMember.getId()).orElseThrow();
		assertThat(leftMember.getStatus()).isEqualTo(TeamMemberStatus.INACTIVE);
		assertThat(userCategoryMembershipRepository.existsByUserIdAndCategory(member.getId(), "GAME")).isFalse();
		assertThat(userCategoryMembershipRepository.existsByUserIdAndCategory(member.getId(), "SPORTS")).isTrue();
		Notification notification = notificationRepository.findAll().get(0);
		assertThat(notification.getReceiver().getId()).isEqualTo(owner.getId());
		assertThat(notification.getType()).isEqualTo(NotificationType.TEAM_MEMBER_LEFT);
		assertThat(notification.getTitle()).isEqualTo("팀원 탈퇴");
		assertThat(notification.getMessage()).isEqualTo("member님이 Game Team 팀에서 탈퇴했습니다.");
		assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.TEAM);
		assertThat(notification.getTargetId()).isEqualTo(savedTeam.getId());

		mockMvc.perform(get("/api/teams/{teamId}/members", savedTeam.getId())
				.header("Authorization", "Bearer " + ownerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].userId").value(owner.getId()));

		userCategoryMembershipRepository.saveAndFlush(UserCategoryMembership.create(member, "GAME", savedTeam));
		assertThat(userCategoryMembershipRepository.existsByUserIdAndCategory(member.getId(), "GAME")).isTrue();
	}

	@Test
	void leaveTeamCreatesNotificationsForRemainingActiveOwnerAndMembersOnly() throws Exception {
		User owner = userRepository.save(User.create("owner-left-notification@test.com", "encoded-password", "owner"));
		User leavingMember = userRepository.save(User.create("leaving-left-notification@test.com", "encoded-password", "leaving"));
		User remainingMember = userRepository.save(User.create("remaining-left-notification@test.com", "encoded-password", "remaining"));
		User inactiveUser = userRepository.save(User.create("inactive-left-notification@test.com", "encoded-password", "inactive"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, leavingMember));
		team.addMember(TeamMember.createMember(team, remainingMember));
		TeamMember inactiveMember = TeamMember.createMember(team, inactiveUser);
		inactiveMember.deactivate();
		team.addMember(inactiveMember);
		Team savedTeam = teamRepository.saveAndFlush(team);
		userCategoryMembershipRepository.saveAndFlush(UserCategoryMembership.create(leavingMember, "GAME", savedTeam));
		String accessToken = jwtTokenProvider.createAccessToken(leavingMember.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/me/leave", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		var notifications = notificationRepository.findAll();
		assertThat(notifications).hasSize(2);
		assertThat(notifications)
			.extracting(notification -> notification.getReceiver().getId())
			.containsExactlyInAnyOrder(owner.getId(), remainingMember.getId());
		assertThat(notifications)
			.extracting(Notification::getType)
			.containsOnly(NotificationType.TEAM_MEMBER_LEFT);
		assertThat(notifications)
			.extracting(Notification::getTargetType)
			.containsOnly(NotificationTargetType.TEAM);
		assertThat(notifications)
			.extracting(Notification::getTargetId)
			.containsOnly(savedTeam.getId());
		assertThat(notifications)
			.extracting(Notification::getMessage)
			.containsOnly("leaving님이 Game Team 팀에서 탈퇴했습니다.");
		assertThat(notifications)
			.extracting(notification -> notification.getReceiver().getId())
			.doesNotContain(leavingMember.getId(), inactiveUser.getId());
	}

	@Test
	void leaveTeamRejectsOwner() throws Exception {
		User owner = userRepository.save(User.create("owner-leave-block@test.com", "encoded-password", "owner"));
		User member = userRepository.save(User.create("member-leave-block@test.com", "encoded-password", "member"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, member));
		Team savedTeam = teamRepository.saveAndFlush(team);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/me/leave", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("T010"));

		TeamMember ownerMember = savedTeam.getMembers().stream()
			.filter(TeamMember::isOwner)
			.findFirst()
			.orElseThrow();
		assertThat(teamMemberRepository.findById(ownerMember.getId()).orElseThrow().getStatus())
			.isEqualTo(TeamMemberStatus.ACTIVE);
		assertThat(notificationRepository.count()).isZero();
	}

	@Test
	void leaveTeamDeletesTeamWhenOwnerIsOnlyActiveMember() throws Exception {
		User owner = userRepository.save(User.create("owner-leave-solo@test.com", "encoded-password", "owner"));
		User otherOwner = userRepository.save(User.create("other-owner-leave-solo@test.com", "encoded-password", "otherOwner"));
		User applicant1 = userRepository.save(User.create("applicant1-leave-solo@test.com", "encoded-password", "applicant1"));
		User applicant2 = userRepository.save(User.create("applicant2-leave-solo@test.com", "encoded-password", "applicant2"));
		User applicant3 = userRepository.save(User.create("applicant3-leave-solo@test.com", "encoded-password", "applicant3"));
		User applicant4 = userRepository.save(User.create("applicant4-leave-solo@test.com", "encoded-password", "applicant4"));
		Team team = teamRepository.saveAndFlush(Team.create("Solo Owner Team", null, "GAME", owner));
		Team sportsTeam = teamRepository.saveAndFlush(Team.create("Sports Team", null, "SPORTS", otherOwner));
		userCategoryMembershipRepository.saveAndFlush(UserCategoryMembership.create(owner, "GAME", team));
		userCategoryMembershipRepository.saveAndFlush(UserCategoryMembership.create(owner, "SPORTS", sportsTeam));
		TeamApplication pendingApplication1 = saveApplication(
			team,
			applicant1,
			"pending1",
			LocalDateTime.of(2026, 5, 22, 10, 0),
			TeamApplicationStatus.PENDING
		);
		TeamApplication pendingApplication2 = saveApplication(
			team,
			applicant2,
			"pending2",
			LocalDateTime.of(2026, 5, 22, 11, 0),
			TeamApplicationStatus.PENDING
		);
		TeamApplication approvedApplication = saveApplication(
			team,
			applicant3,
			"approved",
			LocalDateTime.of(2026, 5, 22, 12, 0),
			TeamApplicationStatus.APPROVED
		);
		TeamApplication canceledApplication = saveApplication(
			team,
			applicant4,
			"canceled",
			LocalDateTime.of(2026, 5, 22, 13, 0),
			TeamApplicationStatus.CANCELED
		);
		TeamApplication rejectedApplication = saveApplication(
			team,
			applicant4,
			"rejected",
			LocalDateTime.of(2026, 5, 22, 14, 0),
			TeamApplicationStatus.REJECTED
		);
		TeamMember ownerMember = team.getMembers().get(0);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/me/leave", team.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		Team deletedTeam = teamRepository.findById(team.getId()).orElseThrow();
		assertThat(deletedTeam.getStatus()).isEqualTo(TeamStatus.DELETED);
		assertThat(deletedTeam.getDeletedAt()).isNotNull();
		assertThat(teamMemberRepository.findById(ownerMember.getId()).orElseThrow().getStatus())
			.isEqualTo(TeamMemberStatus.INACTIVE);
		assertThat(userCategoryMembershipRepository.existsByUserIdAndCategory(owner.getId(), "GAME")).isFalse();
		assertThat(userCategoryMembershipRepository.existsByUserIdAndCategory(owner.getId(), "SPORTS")).isTrue();
		assertThat(teamApplicationRepository.findById(pendingApplication1.getId()).orElseThrow().getStatus())
			.isEqualTo(TeamApplicationStatus.REJECTED);
		assertThat(teamApplicationRepository.findById(pendingApplication2.getId()).orElseThrow().getStatus())
			.isEqualTo(TeamApplicationStatus.REJECTED);
		assertThat(teamApplicationRepository.findById(approvedApplication.getId()).orElseThrow().getStatus())
			.isEqualTo(TeamApplicationStatus.APPROVED);
		assertThat(teamApplicationRepository.findById(canceledApplication.getId()).orElseThrow().getStatus())
			.isEqualTo(TeamApplicationStatus.CANCELED);
		assertThat(teamApplicationRepository.findById(rejectedApplication.getId()).orElseThrow().getStatus())
			.isEqualTo(TeamApplicationStatus.REJECTED);
		assertThat(notificationRepository.count()).isZero();

		mockMvc.perform(get("/api/teams/{teamId}/members", team.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T001"));

		Team newGameTeam = teamRepository.saveAndFlush(Team.create("New Game Team", null, "GAME", otherOwner));
		userCategoryMembershipRepository.saveAndFlush(UserCategoryMembership.create(owner, "GAME", newGameTeam));
		assertThat(userCategoryMembershipRepository.existsByUserIdAndCategory(owner.getId(), "GAME")).isTrue();
	}

	@Test
	void leaveTeamRejectsUserWhoIsNotActiveTeamMember() throws Exception {
		User owner = userRepository.save(User.create("owner-leave-non-member@test.com", "encoded-password", "owner"));
		User outsider = userRepository.save(User.create("outsider-leave@test.com", "encoded-password", "outsider"));
		Team team = teamRepository.saveAndFlush(Team.create("Game Team", null, "GAME", owner));
		String accessToken = jwtTokenProvider.createAccessToken(outsider.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/me/leave", team.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T008"));
		assertThat(notificationRepository.count()).isZero();
	}

	@Test
	void leaveTeamRejectsDeletedTeam() throws Exception {
		User owner = userRepository.save(User.create("owner-leave-deleted@test.com", "encoded-password", "owner"));
		User member = userRepository.save(User.create("member-leave-deleted@test.com", "encoded-password", "member"));
		Team team = Team.create("Deleted Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, member));
		team.softDelete();
		Team savedTeam = teamRepository.saveAndFlush(team);
		String accessToken = jwtTokenProvider.createAccessToken(member.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/me/leave", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T001"));
	}

	@Test
	void leaveTeamRejectsMissingTeam() throws Exception {
		User member = userRepository.save(User.create("member-leave-missing@test.com", "encoded-password", "member"));
		String accessToken = jwtTokenProvider.createAccessToken(member.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/me/leave", 999L)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T001"));
	}

	@Test
	void leaveTeamRejectsRequestWithoutAuthentication() throws Exception {
		User owner = userRepository.save(User.create("owner-leave-auth@test.com", "encoded-password", "owner"));
		Team team = teamRepository.save(Team.create("Game Team", null, "GAME", owner));

		mockMvc.perform(post("/api/teams/{teamId}/members/me/leave", team.getId()))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void kickMemberSucceedsWhenRequesterIsOwner() throws Exception {
		User owner = userRepository.save(User.create("owner-kick@test.com", "encoded-password", "owner"));
		User target = userRepository.save(User.create("target-kick@test.com", "encoded-password", "target"));
		User otherOwner = userRepository.save(User.create("other-owner-kick@test.com", "encoded-password", "otherOwner"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, target));
		Team savedTeam = teamRepository.saveAndFlush(team);
		Team sportsTeam = teamRepository.saveAndFlush(Team.create("Sports Team", null, "SPORTS", otherOwner));
		userCategoryMembershipRepository.saveAndFlush(UserCategoryMembership.create(target, "GAME", savedTeam));
		userCategoryMembershipRepository.saveAndFlush(UserCategoryMembership.create(target, "SPORTS", sportsTeam));
		TeamMember targetMember = savedTeam.getMembers().stream()
			.filter(teamMember -> teamMember.getUser().getId().equals(target.getId()))
			.findFirst()
			.orElseThrow();
		String ownerToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);
		String targetToken = jwtTokenProvider.createAccessToken(target.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/{teamMemberId}/kick", savedTeam.getId(), targetMember.getId())
				.header("Authorization", "Bearer " + ownerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").doesNotExist());

		TeamMember kickedMember = teamMemberRepository.findById(targetMember.getId()).orElseThrow();
		assertThat(kickedMember.getStatus()).isEqualTo(TeamMemberStatus.INACTIVE);
		assertThat(userCategoryMembershipRepository.existsByUserIdAndCategory(target.getId(), "GAME")).isFalse();
		assertThat(userCategoryMembershipRepository.existsByUserIdAndCategory(target.getId(), "SPORTS")).isTrue();
		assertThat(notificationRepository.findAll())
			.extracting(Notification::getType)
			.doesNotContain(NotificationType.TEAM_MEMBER_LEFT);

		mockMvc.perform(get("/api/teams/{teamId}/members", savedTeam.getId())
				.header("Authorization", "Bearer " + ownerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].userId").value(owner.getId()));

		mockMvc.perform(post("/api/teams/{teamId}/applications", savedTeam.getId())
				.header("Authorization", "Bearer " + targetToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new TeamApplicationCreateRequest("다시 가입하고 싶습니다."))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.status").value(TeamApplicationStatus.PENDING.name()));
	}

	@Test
	void kickMemberRejectsRequesterWhoIsNormalMember() throws Exception {
		User owner = userRepository.save(User.create("owner-kick-member@test.com", "encoded-password", "owner"));
		User requester = userRepository.save(User.create("requester-kick-member@test.com", "encoded-password", "requester"));
		User target = userRepository.save(User.create("target-kick-member@test.com", "encoded-password", "target"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, requester));
		team.addMember(TeamMember.createMember(team, target));
		Team savedTeam = teamRepository.saveAndFlush(team);
		TeamMember targetMember = savedTeam.getMembers().stream()
			.filter(teamMember -> teamMember.getUser().getId().equals(target.getId()))
			.findFirst()
			.orElseThrow();
		String accessToken = jwtTokenProvider.createAccessToken(requester.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/{teamMemberId}/kick", savedTeam.getId(), targetMember.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("T002"));
	}

	@Test
	void kickMemberRejectsSelfKick() throws Exception {
		User owner = userRepository.save(User.create("owner-kick-self@test.com", "encoded-password", "owner"));
		Team team = teamRepository.saveAndFlush(Team.create("Game Team", null, "GAME", owner));
		TeamMember ownerMember = team.getMembers().get(0);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/{teamMemberId}/kick", team.getId(), ownerMember.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("T009"));
	}

	@Test
	void kickMemberRejectsOtherTeamMember() throws Exception {
		User owner = userRepository.save(User.create("owner-kick-other-team@test.com", "encoded-password", "owner"));
		User otherOwner = userRepository.save(User.create("other-owner-kick-other-team@test.com", "encoded-password", "otherOwner"));
		User target = userRepository.save(User.create("target-kick-other-team@test.com", "encoded-password", "target"));
		Team team = teamRepository.saveAndFlush(Team.create("Game Team", null, "GAME", owner));
		Team otherTeam = Team.create("Other Team", null, "SPORTS", otherOwner);
		otherTeam.addMember(TeamMember.createMember(otherTeam, target));
		Team savedOtherTeam = teamRepository.saveAndFlush(otherTeam);
		TeamMember otherTeamMember = savedOtherTeam.getMembers().stream()
			.filter(teamMember -> teamMember.getUser().getId().equals(target.getId()))
			.findFirst()
			.orElseThrow();
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/{teamMemberId}/kick", team.getId(), otherTeamMember.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T008"));
	}

	@Test
	void kickMemberRejectsMissingTeam() throws Exception {
		User owner = userRepository.save(User.create("owner-kick-missing-team@test.com", "encoded-password", "owner"));
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/{teamMemberId}/kick", 999L, 1L)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T001"));
	}

	@Test
	void kickMemberRejectsMissingTeamMember() throws Exception {
		User owner = userRepository.save(User.create("owner-kick-missing-member@test.com", "encoded-password", "owner"));
		Team team = teamRepository.saveAndFlush(Team.create("Game Team", null, "GAME", owner));
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/{teamMemberId}/kick", team.getId(), 999L)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T008"));
	}

	@Test
	void kickMemberRejectsInactiveTeamMember() throws Exception {
		User owner = userRepository.save(User.create("owner-kick-inactive@test.com", "encoded-password", "owner"));
		User target = userRepository.save(User.create("target-kick-inactive@test.com", "encoded-password", "target"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		TeamMember inactiveMember = TeamMember.createMember(team, target);
		inactiveMember.deactivate();
		team.addMember(inactiveMember);
		Team savedTeam = teamRepository.saveAndFlush(team);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/{teamMemberId}/kick", savedTeam.getId(), inactiveMember.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T008"));
	}

	@Test
	void kickMemberRejectsDeletedTeam() throws Exception {
		User owner = userRepository.save(User.create("owner-kick-deleted@test.com", "encoded-password", "owner"));
		User target = userRepository.save(User.create("target-kick-deleted@test.com", "encoded-password", "target"));
		Team team = Team.create("Deleted Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, target));
		team.softDelete();
		Team savedTeam = teamRepository.saveAndFlush(team);
		TeamMember targetMember = savedTeam.getMembers().stream()
			.filter(teamMember -> teamMember.getUser().getId().equals(target.getId()))
			.findFirst()
			.orElseThrow();
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/{teamMemberId}/kick", savedTeam.getId(), targetMember.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T001"));
	}

	@Test
	void kickMemberRejectsOwnerTarget() throws Exception {
		User owner = userRepository.save(User.create("owner-kick-owner-target@test.com", "encoded-password", "owner"));
		User targetOwner = userRepository.save(User.create("target-owner-kick@test.com", "encoded-password", "targetOwner"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		TeamMember targetOwnerMember = TeamMember.createOwner(team, targetOwner);
		team.addMember(targetOwnerMember);
		Team savedTeam = teamRepository.saveAndFlush(team);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/members/{teamMemberId}/kick", savedTeam.getId(), targetOwnerMember.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("T012"));
	}

	@Test
	void kickMemberRejectsRequestWithoutAuthentication() throws Exception {
		User owner = userRepository.save(User.create("owner-kick-auth@test.com", "encoded-password", "owner"));
		User target = userRepository.save(User.create("target-kick-auth@test.com", "encoded-password", "target"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, target));
		Team savedTeam = teamRepository.saveAndFlush(team);
		TeamMember targetMember = savedTeam.getMembers().stream()
			.filter(teamMember -> teamMember.getUser().getId().equals(target.getId()))
			.findFirst()
			.orElseThrow();

		mockMvc.perform(post("/api/teams/{teamId}/members/{teamMemberId}/kick", savedTeam.getId(), targetMember.getId()))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void delegateOwnerSucceeds() throws Exception {
		User owner = userRepository.save(User.create("owner-delegate@test.com", "encoded-password", "owner"));
		User target = userRepository.save(User.create("target-delegate@test.com", "encoded-password", "target"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, target));
		Team savedTeam = teamRepository.saveAndFlush(team);
		TeamMember ownerMember = savedTeam.getMembers().stream()
			.filter(TeamMember::isOwner)
			.findFirst()
			.orElseThrow();
		TeamMember targetMember = savedTeam.getMembers().stream()
			.filter(teamMember -> teamMember.getUser().getId().equals(target.getId()))
			.findFirst()
			.orElseThrow();
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/owner/delegate", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new OwnerDelegationRequest(targetMember.getId()))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").doesNotExist());

		assertThat(teamMemberRepository.findById(ownerMember.getId()).orElseThrow().getRole())
			.isEqualTo(com.neomango.team.entity.TeamMemberRole.MEMBER);
		assertThat(teamMemberRepository.findById(targetMember.getId()).orElseThrow().getRole())
			.isEqualTo(com.neomango.team.entity.TeamMemberRole.OWNER);
		assertThat(teamMemberRepository.countActiveOwnersByTeamId(savedTeam.getId())).isEqualTo(1);

		String targetToken = jwtTokenProvider.createAccessToken(target.getId(), UserRole.USER);
		mockMvc.perform(get("/api/teams/{teamId}/members", savedTeam.getId())
				.header("Authorization", "Bearer " + targetToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].userId").value(owner.getId()))
			.andExpect(jsonPath("$.data[0].role").value("MEMBER"))
			.andExpect(jsonPath("$.data[1].userId").value(target.getId()))
			.andExpect(jsonPath("$.data[1].role").value("OWNER"));
	}

	@Test
	void delegateOwnerRejectsRequesterWhoIsNormalMember() throws Exception {
		User owner = userRepository.save(User.create("owner-delegate-member@test.com", "encoded-password", "owner"));
		User requester = userRepository.save(User.create("requester-delegate-member@test.com", "encoded-password", "requester"));
		User target = userRepository.save(User.create("target-delegate-member@test.com", "encoded-password", "target"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, requester));
		team.addMember(TeamMember.createMember(team, target));
		Team savedTeam = teamRepository.saveAndFlush(team);
		TeamMember targetMember = savedTeam.getMembers().stream()
			.filter(teamMember -> teamMember.getUser().getId().equals(target.getId()))
			.findFirst()
			.orElseThrow();
		String accessToken = jwtTokenProvider.createAccessToken(requester.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/owner/delegate", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new OwnerDelegationRequest(targetMember.getId()))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("T002"));
	}

	@Test
	void delegateOwnerRejectsSelfDelegation() throws Exception {
		User owner = userRepository.save(User.create("owner-delegate-self@test.com", "encoded-password", "owner"));
		Team team = teamRepository.saveAndFlush(Team.create("Game Team", null, "GAME", owner));
		TeamMember ownerMember = team.getMembers().get(0);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/owner/delegate", team.getId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new OwnerDelegationRequest(ownerMember.getId()))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("T011"));
	}

	@Test
	void delegateOwnerRejectsOtherTeamMember() throws Exception {
		User owner = userRepository.save(User.create("owner-delegate-other-team@test.com", "encoded-password", "owner"));
		User otherOwner = userRepository.save(User.create("other-owner-delegate@test.com", "encoded-password", "otherOwner"));
		User target = userRepository.save(User.create("target-delegate-other-team@test.com", "encoded-password", "target"));
		Team team = teamRepository.saveAndFlush(Team.create("Game Team", null, "GAME", owner));
		Team otherTeam = Team.create("Other Team", null, "SPORTS", otherOwner);
		otherTeam.addMember(TeamMember.createMember(otherTeam, target));
		Team savedOtherTeam = teamRepository.saveAndFlush(otherTeam);
		TeamMember otherTeamMember = savedOtherTeam.getMembers().stream()
			.filter(teamMember -> teamMember.getUser().getId().equals(target.getId()))
			.findFirst()
			.orElseThrow();
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/owner/delegate", team.getId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new OwnerDelegationRequest(otherTeamMember.getId()))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T008"));
	}

	@Test
	void delegateOwnerRejectsMissingTeam() throws Exception {
		User owner = userRepository.save(User.create("owner-delegate-missing-team@test.com", "encoded-password", "owner"));
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/owner/delegate", 999L)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new OwnerDelegationRequest(1L))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T001"));
	}

	@Test
	void delegateOwnerRejectsMissingTeamMember() throws Exception {
		User owner = userRepository.save(User.create("owner-delegate-missing-member@test.com", "encoded-password", "owner"));
		Team team = teamRepository.saveAndFlush(Team.create("Game Team", null, "GAME", owner));
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/owner/delegate", team.getId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new OwnerDelegationRequest(999L))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T008"));
	}

	@Test
	void delegateOwnerRejectsInactiveTeamMember() throws Exception {
		User owner = userRepository.save(User.create("owner-delegate-inactive@test.com", "encoded-password", "owner"));
		User target = userRepository.save(User.create("target-delegate-inactive@test.com", "encoded-password", "target"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		TeamMember inactiveMember = TeamMember.createMember(team, target);
		inactiveMember.deactivate();
		team.addMember(inactiveMember);
		Team savedTeam = teamRepository.saveAndFlush(team);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/owner/delegate", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new OwnerDelegationRequest(inactiveMember.getId()))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T008"));
	}

	@Test
	void delegateOwnerRejectsOwnerTarget() throws Exception {
		User owner = userRepository.save(User.create("owner-delegate-owner-target@test.com", "encoded-password", "owner"));
		User targetOwner = userRepository.save(User.create("target-owner-delegate@test.com", "encoded-password", "targetOwner"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		TeamMember targetOwnerMember = TeamMember.createOwner(team, targetOwner);
		team.addMember(targetOwnerMember);
		Team savedTeam = teamRepository.saveAndFlush(team);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/owner/delegate", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new OwnerDelegationRequest(targetOwnerMember.getId()))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("T011"));
	}

	@Test
	void delegateOwnerRejectsDeletedTeam() throws Exception {
		User owner = userRepository.save(User.create("owner-delegate-deleted@test.com", "encoded-password", "owner"));
		User target = userRepository.save(User.create("target-delegate-deleted@test.com", "encoded-password", "target"));
		Team team = Team.create("Deleted Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, target));
		team.softDelete();
		Team savedTeam = teamRepository.saveAndFlush(team);
		TeamMember targetMember = savedTeam.getMembers().stream()
			.filter(teamMember -> teamMember.getUser().getId().equals(target.getId()))
			.findFirst()
			.orElseThrow();
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/owner/delegate", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new OwnerDelegationRequest(targetMember.getId()))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T001"));
	}

	@Test
	void delegateOwnerRejectsWhenActiveOwnerCountIsNotOneAfterDelegation() throws Exception {
		User owner = userRepository.save(User.create("owner-delegate-count@test.com", "encoded-password", "owner"));
		User anotherOwner = userRepository.save(User.create("another-owner-delegate-count@test.com", "encoded-password", "anotherOwner"));
		User target = userRepository.save(User.create("target-delegate-count@test.com", "encoded-password", "target"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		TeamMember anotherOwnerMember = TeamMember.createOwner(team, anotherOwner);
		TeamMember targetMember = TeamMember.createMember(team, target);
		team.addMember(anotherOwnerMember);
		team.addMember(targetMember);
		Team savedTeam = teamRepository.saveAndFlush(team);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(post("/api/teams/{teamId}/owner/delegate", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new OwnerDelegationRequest(targetMember.getId()))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("T013"));
		assertThat(teamMemberRepository.countActiveOwnersByTeamId(savedTeam.getId())).isEqualTo(2);
	}

	@Test
	void delegateOwnerRejectsRequestWithoutAuthentication() throws Exception {
		User owner = userRepository.save(User.create("owner-delegate-auth@test.com", "encoded-password", "owner"));
		User target = userRepository.save(User.create("target-delegate-auth@test.com", "encoded-password", "target"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, target));
		Team savedTeam = teamRepository.saveAndFlush(team);
		TeamMember targetMember = savedTeam.getMembers().stream()
			.filter(teamMember -> teamMember.getUser().getId().equals(target.getId()))
			.findFirst()
			.orElseThrow();

		mockMvc.perform(post("/api/teams/{teamId}/owner/delegate", savedTeam.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new OwnerDelegationRequest(targetMember.getId()))))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void closeTeamSucceedsWhenRequesterIsOwner() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		Team team = teamRepository.save(Team.create("Game Team", null, "GAME", owner));
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(patch("/api/teams/{teamId}/close", team.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		Team closedTeam = teamRepository.findById(team.getId()).orElseThrow();
		assertThat(closedTeam.getStatus()).isEqualTo(TeamStatus.CLOSED);
	}

	@Test
	void closeTeamRejectsRequestWithoutAuthentication() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		Team team = teamRepository.save(Team.create("Game Team", null, "GAME", owner));

		mockMvc.perform(patch("/api/teams/{teamId}/close", team.getId()))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void closeTeamRejectsRequesterWhoIsNotOwner() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		User member = userRepository.save(User.create("member@test.com", "encoded-password", "member"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, member));
		Team savedTeam = teamRepository.save(team);
		String accessToken = jwtTokenProvider.createAccessToken(member.getId(), UserRole.USER);

		mockMvc.perform(patch("/api/teams/{teamId}/close", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("T002"));
	}

	@Test
	void deleteTeamSucceedsWhenRequesterIsOwner() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		Team team = teamRepository.save(Team.create("Game Team", null, "GAME", owner));
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(delete("/api/teams/{teamId}", team.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		Team deletedTeam = teamRepository.findById(team.getId()).orElseThrow();
		assertThat(deletedTeam.getDeletedAt()).isNotNull();
		assertThat(teamMemberRepository.count()).isEqualTo(1);
	}

	@Test
	void deleteTeamRejectsRequestWithoutAuthentication() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		Team team = teamRepository.save(Team.create("Game Team", null, "GAME", owner));

		mockMvc.perform(delete("/api/teams/{teamId}", team.getId()))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void deleteTeamRejectsRequesterWhoIsNotOwner() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		User member = userRepository.save(User.create("member@test.com", "encoded-password", "member"));
		Team team = Team.create("Game Team", null, "GAME", owner);
		team.addMember(TeamMember.createMember(team, member));
		Team savedTeam = teamRepository.save(team);
		String accessToken = jwtTokenProvider.createAccessToken(member.getId(), UserRole.USER);

		mockMvc.perform(delete("/api/teams/{teamId}", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("T002"));
	}

	@Test
	void deletedTeamIsExcludedFromListAndDetail() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		Team team = teamRepository.save(Team.create("Game Team", null, "GAME", owner));
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(delete("/api/teams/{teamId}", team.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/teams")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(0));

		mockMvc.perform(get("/api/teams/{teamId}", team.getId()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T001"));
	}

	@Test
	void createTeamApplicationReturnsCreatedWhenAuthenticated() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		User applicant = userRepository.save(User.create("applicant@test.com", "encoded-password", "applicant"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		String accessToken = jwtTokenProvider.createAccessToken(applicant.getId(), UserRole.USER);
		TeamApplicationCreateRequest request = new TeamApplicationCreateRequest("가입하고 싶습니다.");

		mockMvc.perform(post("/api/teams/{teamId}/applications", team.getId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.applicationId").exists())
			.andExpect(jsonPath("$.data.teamId").value(team.getId()))
			.andExpect(jsonPath("$.data.teamName").value("Futsal Team"))
			.andExpect(jsonPath("$.data.status").value(TeamApplicationStatus.PENDING.name()))
			.andExpect(jsonPath("$.data.message").value("가입하고 싶습니다."));

		TeamApplication application = teamApplicationRepository.findAll().get(0);
		Notification notification = notificationRepository.findAll().get(0);
		assertThat(notification.getReceiver().getId()).isEqualTo(owner.getId());
		assertThat(notification.getType()).isEqualTo(NotificationType.TEAM_APPLICATION_CREATED);
		assertThat(notification.getTitle()).isEqualTo("팀 가입 신청");
		assertThat(notification.getMessage()).isEqualTo("applicant님이 Futsal Team 팀에 가입 신청했습니다.");
		assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.TEAM_APPLICATION);
		assertThat(notification.getTargetId()).isEqualTo(application.getId());
	}

	@Test
	void createTeamApplicationRejectsRequestWithoutAuthentication() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		TeamApplicationCreateRequest request = new TeamApplicationCreateRequest("가입하고 싶습니다.");

		mockMvc.perform(post("/api/teams/{teamId}/applications", team.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());
		assertThat(notificationRepository.count()).isZero();
	}

	@Test
	void getTeamApplicationsReturnsPendingApplicationsWhenRequesterIsOwner() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		User applicant1 = userRepository.save(User.create("applicant1@test.com", "encoded-password", "mango1"));
		User applicant2 = userRepository.save(User.create("applicant2@test.com", "encoded-password", "mango2"));
		User applicant3 = userRepository.save(User.create("applicant3@test.com", "encoded-password", "mango3"));
		User applicant4 = userRepository.save(User.create("applicant4@test.com", "encoded-password", "mango4"));
		User applicant5 = userRepository.save(User.create("applicant5@test.com", "encoded-password", "mango5"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		saveApplication(team, applicant2, "second", LocalDateTime.of(2026, 5, 21, 13, 0), TeamApplicationStatus.PENDING);
		saveApplication(team, applicant1, "first", LocalDateTime.of(2026, 5, 21, 12, 0), TeamApplicationStatus.PENDING);
		saveApplication(team, applicant3, "approved", LocalDateTime.of(2026, 5, 21, 11, 0), TeamApplicationStatus.APPROVED);
		saveApplication(team, applicant4, "rejected", LocalDateTime.of(2026, 5, 21, 10, 0), TeamApplicationStatus.REJECTED);
		saveApplication(team, applicant5, "canceled", LocalDateTime.of(2026, 5, 21, 9, 0), TeamApplicationStatus.CANCELED);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(get("/api/teams/{teamId}/applications", team.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].applicantId").value(applicant1.getId()))
			.andExpect(jsonPath("$.data[0].applicantNickname").value("mango1"))
			.andExpect(jsonPath("$.data[0].teamId").value(team.getId()))
			.andExpect(jsonPath("$.data[0].teamName").value("Futsal Team"))
			.andExpect(jsonPath("$.data[0].status").value("PENDING"))
			.andExpect(jsonPath("$.data[0].message").value("first"))
			.andExpect(jsonPath("$.data[0].createdAt").exists())
			.andExpect(jsonPath("$.data[1].applicantId").value(applicant2.getId()))
			.andExpect(jsonPath("$.data[1].applicantNickname").value("mango2"))
			.andExpect(jsonPath("$.data[1].message").value("second"));
	}

	@Test
	void getTeamApplicationsRejectsNormalMember() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		User member = userRepository.save(User.create("member@test.com", "encoded-password", "member"));
		Team team = Team.create("Futsal Team", null, "FUTSAL", owner);
		team.addMember(TeamMember.createMember(team, member));
		Team savedTeam = teamRepository.save(team);
		String accessToken = jwtTokenProvider.createAccessToken(member.getId(), UserRole.USER);

		mockMvc.perform(get("/api/teams/{teamId}/applications", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("TA006"));
	}

	@Test
	void getTeamApplicationsRejectsUserWhoDoesNotBelongToTeam() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		User outsider = userRepository.save(User.create("outsider@test.com", "encoded-password", "outsider"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		String accessToken = jwtTokenProvider.createAccessToken(outsider.getId(), UserRole.USER);

		mockMvc.perform(get("/api/teams/{teamId}/applications", team.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("TA006"));
	}

	@Test
	void getTeamApplicationsRejectsOtherTeamOwner() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		User otherOwner = userRepository.save(User.create("other-owner@test.com", "encoded-password", "otherOwner"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		teamRepository.save(Team.create("Other Team", null, "GAME", otherOwner));
		String accessToken = jwtTokenProvider.createAccessToken(otherOwner.getId(), UserRole.USER);

		mockMvc.perform(get("/api/teams/{teamId}/applications", team.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("TA006"));
	}

	@Test
	void getTeamApplicationsReturnsNotFoundWhenTeamDoesNotExist() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(get("/api/teams/{teamId}/applications", 999L)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T001"));
	}

	@Test
	void getTeamApplicationsReturnsNotFoundWhenTeamIsDeleted() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		Team team = Team.create("Futsal Team", null, "FUTSAL", owner);
		team.softDelete();
		Team savedTeam = teamRepository.save(team);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(get("/api/teams/{teamId}/applications", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T001"));
	}

	@Test
	void getTeamApplicationsReturnsNotFoundWhenTeamDeletedAtExists() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		Team team = Team.create("Futsal Team", null, "FUTSAL", owner);
		ReflectionTestUtils.setField(team, "deletedAt", LocalDateTime.now());
		Team savedTeam = teamRepository.save(team);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(get("/api/teams/{teamId}/applications", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("T001"));
	}

	@Test
	void getTeamApplicationsAllowsClosedTeam() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		User applicant = userRepository.save(User.create("applicant@test.com", "encoded-password", "applicant"));
		Team team = Team.create("Futsal Team", null, "FUTSAL", owner);
		team.close();
		Team savedTeam = teamRepository.save(team);
		saveApplication(savedTeam, applicant, "pending", LocalDateTime.of(2026, 5, 21, 12, 0), TeamApplicationStatus.PENDING);
		String accessToken = jwtTokenProvider.createAccessToken(owner.getId(), UserRole.USER);

		mockMvc.perform(get("/api/teams/{teamId}/applications", savedTeam.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].applicantId").value(applicant.getId()));
	}

	@Test
	void getTeamApplicationsRejectsRequestWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/teams/{teamId}/applications", 1L))
			.andExpect(status().isUnauthorized());
	}

	private TeamApplication saveApplication(
		Team team,
		User applicant,
		String message,
		LocalDateTime createdAt,
		TeamApplicationStatus status
	) {
		TeamApplication application = TeamApplication.create(team, applicant, message);
		switch (status) {
			case APPROVED -> application.approve();
			case REJECTED -> application.reject();
			case CANCELED -> application.cancel();
			case PENDING -> {
			}
		}
		teamApplicationRepository.saveAndFlush(application);
		ReflectionTestUtils.setField(application, "createdAt", createdAt);
		ReflectionTestUtils.setField(application, "updatedAt", createdAt);
		return teamApplicationRepository.saveAndFlush(application);
	}
}
