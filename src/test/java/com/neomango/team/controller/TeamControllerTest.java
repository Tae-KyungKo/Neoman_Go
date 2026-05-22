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
import com.neomango.team.dto.TeamApplicationCreateRequest;
import com.neomango.team.dto.TeamCreateRequest;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamApplicationStatus;
import com.neomango.team.entity.TeamStatus;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
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

	@BeforeEach
	void setUp() {
		teamApplicationRepository.deleteAll();
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
