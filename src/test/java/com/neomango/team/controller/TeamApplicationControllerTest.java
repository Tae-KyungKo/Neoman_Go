package com.neomango.team.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.neomango.auth.jwt.JwtTokenProvider;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamApplicationStatus;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
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
	private TeamApplicationRepository teamApplicationRepository;

	@BeforeEach
	void setUp() {
		teamApplicationRepository.deleteAll();
		teamMemberRepository.deleteAll();
		teamRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void cancelTeamApplicationReturnsCanceledApplicationWhenAuthenticatedApplicantRequests() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		User applicant = userRepository.save(User.create("applicant@test.com", "encoded-password", "applicant"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", 5, owner));
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
}
