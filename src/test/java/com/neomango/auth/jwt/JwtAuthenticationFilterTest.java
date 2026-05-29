package com.neomango.auth.jwt;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neomango.global.response.ApiResponse;
import com.neomango.global.security.AuthenticatedUser;
import com.neomango.user.entity.UserRole;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Import(JwtAuthenticationFilterTest.TestAuthController.class)
class JwtAuthenticationFilterTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void validAccessTokenAuthenticatesRequest() throws Exception {
		String accessToken = jwtTokenProvider.createAccessToken(1L, UserRole.USER);

		mockMvc.perform(get("/api/test/auth")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.userId", is(1)))
			.andExpect(jsonPath("$.data.role", is("USER")));
	}

	@Test
	void missingAuthorizationHeaderDoesNotAuthenticateRequest() throws Exception {
		mockMvc.perform(get("/api/test/auth"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void authorizationHeaderWithoutBearerPrefixDoesNotAuthenticateRequest() throws Exception {
		String accessToken = jwtTokenProvider.createAccessToken(1L, UserRole.USER);

		mockMvc.perform(get("/api/test/auth")
				.header("Authorization", accessToken))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void invalidJwtDoesNotAuthenticateRequest() throws Exception {
		mockMvc.perform(get("/api/test/auth")
				.header("Authorization", "Bearer invalid-token"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void expiredTokenDoesNotAuthenticateRequest() throws Exception {
		JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(
			new JwtProperties("neomango-test-jwt-secret-key-over-32-bytes", -1, 1209600)
		);
		String expiredToken = expiredTokenProvider.createAccessToken(1L, UserRole.USER);

		mockMvc.perform(get("/api/test/auth")
				.header("Authorization", "Bearer " + expiredToken))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void refreshTokenDoesNotAuthenticateRequest() throws Exception {
		String refreshToken = jwtTokenProvider.createRefreshToken(1L);

		mockMvc.perform(get("/api/test/auth")
				.header("Authorization", "Bearer " + refreshToken))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void permitAllApiAllowsRequestWithoutToken() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType("application/json")
				.content("{}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void permitAllLoginIgnoresInvalidAuthorizationHeader() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.header("Authorization", "Bearer invalid-token")
				.contentType("application/json")
				.content("{}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void permitAllSignupAllowsRequestWithoutToken() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType("application/json")
				.content("{}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void permitAllReissueAllowsRequestWithoutToken() throws Exception {
		mockMvc.perform(post("/api/auth/reissue")
				.contentType("application/json")
				.content("{}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void protectedApiRejectsRequestWithoutToken() throws Exception {
		mockMvc.perform(get("/api/test/auth"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutApiRejectsRequestWithoutToken() throws Exception {
		mockMvc.perform(post("/api/auth/logout"))
			.andExpect(status().isUnauthorized());
	}

	@RestController
	static class TestAuthController {

		@GetMapping("/api/test/auth")
		ApiResponse<TestAuthResponse> auth(@AuthenticationPrincipal AuthenticatedUser currentUser) {
			return ApiResponse.success(new TestAuthResponse(currentUser.userId(), currentUser.role()));
		}
	}

	private record TestAuthResponse(
		Long userId,
		String role
	) {
	}
}
