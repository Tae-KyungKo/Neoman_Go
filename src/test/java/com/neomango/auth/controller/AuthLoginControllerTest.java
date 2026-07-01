package com.neomango.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.neomango.auth.dto.LoginRequest;
import com.neomango.auth.dto.TokenResponse;
import com.neomango.auth.service.AuthService;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AuthLoginControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@Test
	void loginSucceedsWithLoginIdAndPassword() throws Exception {
		when(authService.login(any(LoginRequest.class)))
			.thenReturn(new TokenResponse("access-token", "refresh-token", "Bearer", 1800L));

		mockMvc.perform(post("/api/auth/login")
				.contentType("application/json")
				.content("""
					{
						"loginId": "tester01",
						"password": "Password123!"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.accessTokenExpiresIn").value(1800));
	}

	@Test
	void loginRejectsLegacyEmailOnlyRequest() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType("application/json")
				.content("""
					{
						"email": "tester@example.com",
						"password": "Password123!"
					}
					"""))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(authService);
	}

	@Test
	void loginRejectsInvalidLoginId() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType("application/json")
				.content("""
					{
						"loginId": "tester01!",
						"password": "Password123!"
					}
					"""))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(authService);
	}

	@Test
	void loginRejectsMissingPassword() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType("application/json")
				.content("""
					{
						"loginId": "tester01"
					}
					"""))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(authService);
	}
}
