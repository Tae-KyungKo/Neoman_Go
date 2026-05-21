package com.neomango.team.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.neomango.auth.jwt.JwtTokenProvider;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamApplication;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;
import com.neomango.user.repository.UserRepository;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class MyTeamApplicationControllerTest {

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
	private TeamApplicationRepository teamApplicationRepository;

	@BeforeEach
	void setUp() {
		teamApplicationRepository.deleteAll();
		teamMemberRepository.deleteAll();
		teamRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void getMyTeamApplicationsReturnsOnlyCurrentUserApplicationsOrderedByCreatedAtDesc() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		User applicant = userRepository.save(User.create("applicant@test.com", "encoded-password", "applicant"));
		User otherApplicant = userRepository.save(User.create("other@test.com", "encoded-password", "other"));
		Team pendingTeam = teamRepository.save(Team.create("Pending Team", null, "FOOTBALL", 5, owner));
		Team approvedTeam = teamRepository.save(Team.create("Approved Team", null, "BASEBALL", 5, owner));
		Team rejectedTeam = teamRepository.save(Team.create("Rejected Team", null, "BASKETBALL", 5, owner));
		Team canceledTeam = teamRepository.save(Team.create("Canceled Team", null, "TENNIS", 5, owner));
		Team deletedTeam = teamRepository.save(Team.create("Deleted Team", null, "FUTSAL", 5, owner));
		Team otherTeam = teamRepository.save(Team.create("Other Team", null, "GAME", 5, owner));

		saveApplication(pendingTeam, applicant, "pending", LocalDateTime.of(2026, 5, 21, 12, 0), "PENDING");
		saveApplication(approvedTeam, applicant, "approved", LocalDateTime.of(2026, 5, 21, 13, 0), "APPROVED");
		saveApplication(rejectedTeam, applicant, "rejected", LocalDateTime.of(2026, 5, 21, 14, 0), "REJECTED");
		saveApplication(canceledTeam, applicant, "canceled", LocalDateTime.of(2026, 5, 21, 15, 0), "CANCELED");
		saveApplication(deletedTeam, applicant, "deleted", LocalDateTime.of(2026, 5, 21, 16, 0), "PENDING");
		deletedTeam.softDelete();
		teamRepository.saveAndFlush(deletedTeam);
		saveApplication(otherTeam, otherApplicant, "other", LocalDateTime.of(2026, 5, 21, 17, 0), "PENDING");
		String accessToken = jwtTokenProvider.createAccessToken(applicant.getId(), UserRole.USER);

		mockMvc.perform(get("/api/me/team-applications")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(5))
			.andExpect(jsonPath("$.data[0].teamName").value("Deleted Team"))
			.andExpect(jsonPath("$.data[0].status").value("PENDING"))
			.andExpect(jsonPath("$.data[0].category").value("FUTSAL"))
			.andExpect(jsonPath("$.data[0].createdAt").exists())
			.andExpect(jsonPath("$.data[1].teamName").value("Canceled Team"))
			.andExpect(jsonPath("$.data[1].status").value("CANCELED"))
			.andExpect(jsonPath("$.data[2].teamName").value("Rejected Team"))
			.andExpect(jsonPath("$.data[2].status").value("REJECTED"))
			.andExpect(jsonPath("$.data[3].teamName").value("Approved Team"))
			.andExpect(jsonPath("$.data[3].status").value("APPROVED"))
			.andExpect(jsonPath("$.data[4].teamName").value("Pending Team"))
			.andExpect(jsonPath("$.data[4].status").value("PENDING"));
	}

	@Test
	void getMyTeamApplicationsRejectsRequestWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/me/team-applications"))
			.andExpect(status().isUnauthorized());
	}

	private void saveApplication(
		Team team,
		User applicant,
		String message,
		LocalDateTime createdAt,
		String status
	) {
		TeamApplication application = TeamApplication.create(team, applicant, message);
		switch (status) {
			case "APPROVED" -> application.approve();
			case "REJECTED" -> application.reject();
			case "CANCELED" -> application.cancel();
			case "PENDING" -> {
			}
			default -> throw new IllegalArgumentException("Unsupported status: " + status);
		}
		teamApplicationRepository.saveAndFlush(application);
		ReflectionTestUtils.setField(application, "createdAt", createdAt);
		ReflectionTestUtils.setField(application, "updatedAt", createdAt);
		teamApplicationRepository.saveAndFlush(application);
		assertThat(application.getId()).isNotNull();
	}
}
