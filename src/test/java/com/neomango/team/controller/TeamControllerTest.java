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
import com.neomango.team.dto.TeamCreateRequest;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamStatus;
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

	@BeforeEach
	void setUp() {
		teamMemberRepository.deleteAll();
		teamRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void createTeamReturnsCreatedWhenAuthenticated() throws Exception {
		User user = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), UserRole.USER);
		TeamCreateRequest request = new TeamCreateRequest("Game Team", "Weekend game team", "GAME", 5);

		mockMvc.perform(post("/api/teams")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.name").value("Game Team"))
			.andExpect(jsonPath("$.data.description").value("Weekend game team"))
			.andExpect(jsonPath("$.data.category").value("GAME"))
			.andExpect(jsonPath("$.data.maxMemberCount").value(5))
			.andExpect(jsonPath("$.data.currentMemberCount").value(1))
			.andExpect(jsonPath("$.data.status").value("RECRUITING"))
			.andExpect(jsonPath("$.data.ownerId").value(user.getId()))
			.andExpect(jsonPath("$.data.ownerNickname").value("owner"));
	}

	@Test
	void createTeamRejectsRequestWithoutAuthentication() throws Exception {
		TeamCreateRequest request = new TeamCreateRequest("Game Team", "Weekend game team", "GAME", 5);

		mockMvc.perform(post("/api/teams")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void createTeamReturnsBadRequestWhenNameIsBlank() throws Exception {
		User user = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), UserRole.USER);
		TeamCreateRequest request = new TeamCreateRequest(" ", "Weekend game team", "GAME", 5);

		mockMvc.perform(post("/api/teams")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));
	}

	@Test
	void createTeamReturnsBadRequestWhenMaxMemberCountIsOne() throws Exception {
		User user = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), UserRole.USER);
		TeamCreateRequest request = new TeamCreateRequest("Game Team", "Weekend game team", "GAME", 1);

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
		teamRepository.save(Team.create("Game Team", "Weekend game team", "GAME", 5, owner));

		mockMvc.perform(get("/api/teams")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content[0].name").value("Game Team"))
			.andExpect(jsonPath("$.data.content[0].category").value("GAME"))
			.andExpect(jsonPath("$.data.content[0].currentMemberCount").value(1))
			.andExpect(jsonPath("$.data.content[0].maxMemberCount").value(5))
			.andExpect(jsonPath("$.data.content[0].status").value("RECRUITING"))
			.andExpect(jsonPath("$.data.content[0].ownerId").value(owner.getId()))
			.andExpect(jsonPath("$.data.content[0].ownerNickname").value("owner"));
	}

	@Test
	void getTeamsFiltersByCategoryWithoutAuthentication() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		teamRepository.save(Team.create("Game Team", null, "GAME", 5, owner));
		teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", 5, owner));

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
		Team olderTeam = teamRepository.save(Team.create("Older Team", null, "GAME", 5, owner));
		Team newerTeam = teamRepository.save(Team.create("Newer Team", null, "GAME", 5, owner));
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
		teamRepository.save(Team.create("Active Team", null, "GAME", 5, owner));
		Team deletedTeam = Team.create("Deleted Team", null, "GAME", 5, owner);
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
		Team team = teamRepository.save(Team.create("Game Team", "Weekend game team", "GAME", 5, owner));

		mockMvc.perform(get("/api/teams/{teamId}", team.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(team.getId()))
			.andExpect(jsonPath("$.data.name").value("Game Team"))
			.andExpect(jsonPath("$.data.description").value("Weekend game team"))
			.andExpect(jsonPath("$.data.category").value("GAME"))
			.andExpect(jsonPath("$.data.currentMemberCount").value(1))
			.andExpect(jsonPath("$.data.maxMemberCount").value(5))
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
		Team team = teamRepository.save(Team.create("Game Team", null, "GAME", 5, owner));
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
		Team team = teamRepository.save(Team.create("Game Team", null, "GAME", 5, owner));

		mockMvc.perform(patch("/api/teams/{teamId}/close", team.getId()))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void closeTeamRejectsRequesterWhoIsNotOwner() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		User member = userRepository.save(User.create("member@test.com", "encoded-password", "member"));
		Team team = Team.create("Game Team", null, "GAME", 5, owner);
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
		Team team = teamRepository.save(Team.create("Game Team", null, "GAME", 5, owner));
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
		Team team = teamRepository.save(Team.create("Game Team", null, "GAME", 5, owner));

		mockMvc.perform(delete("/api/teams/{teamId}", team.getId()))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void deleteTeamRejectsRequesterWhoIsNotOwner() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		User member = userRepository.save(User.create("member@test.com", "encoded-password", "member"));
		Team team = Team.create("Game Team", null, "GAME", 5, owner);
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
		Team team = teamRepository.save(Team.create("Game Team", null, "GAME", 5, owner));
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
}
