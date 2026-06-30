package com.neomango.auth.jwt;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.GrantedAuthority;
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
			.andExpect(jsonPath("$.data.role", is("USER")))
			.andExpect(jsonPath("$.data.authorities", contains("ROLE_USER")));
	}

	@Test
	void adminAccessTokenCreatesRoleAdminAuthority() throws Exception {
		String accessToken = jwtTokenProvider.createAccessToken(2L, UserRole.ADMIN);

		mockMvc.perform(get("/api/test/auth")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.userId", is(2)))
			.andExpect(jsonPath("$.data.role", is("ADMIN")))
			.andExpect(jsonPath("$.data.authorities", contains("ROLE_ADMIN")));
	}

	@Test
	void adminApiRejectsRequestWithoutToken() throws Exception {
		mockMvc.perform(get("/api/admin/test"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void adminApiRejectsUserRoleToken() throws Exception {
		String accessToken = jwtTokenProvider.createAccessToken(1L, UserRole.USER);

		mockMvc.perform(get("/api/admin/test")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminApiAllowsAdminRoleToken() throws Exception {
		String accessToken = jwtTokenProvider.createAccessToken(2L, UserRole.ADMIN);

		mockMvc.perform(get("/api/admin/test")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.role", is("ADMIN")))
			.andExpect(jsonPath("$.data.authorities", contains("ROLE_ADMIN")));
	}

	@Test
	void signupIgnoresInjectedAdminRole() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType("application/json")
				.content("""
					{
						"loginId": "tester01",
						"email": "admin-injection@test.com",
						"password": "Password123!",
						"passwordConfirm": "Password123!",
						"nickname": "normalUser",
						"role": "ADMIN"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.role", is("USER")));
	}

	@Test
	void signupRejectsLegacyRequestWithoutLoginIdAndPasswordConfirm() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType("application/json")
				.content("""
					{
						"email": "old@example.com",
						"password": "Password123!",
						"nickname": "olduser"
					}
					"""))
			.andExpect(status().isBadRequest());
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

	@Test
	void actuatorHealthAllowsRequestWithoutToken() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("UP")));
	}

	@Test
	void actuatorInfoAllowsRequestWithoutToken() throws Exception {
		mockMvc.perform(get("/actuator/info"))
			.andExpect(status().isOk());
	}

	@Test
	void actuatorEnvIsNotExposed() throws Exception {
		mockMvc.perform(get("/actuator/env"))
			.andExpect(status().isUnauthorized());
	}

	@RestController
	static class TestAuthController {

		@GetMapping("/api/test/auth")
		ApiResponse<TestAuthResponse> auth(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			Authentication authentication
		) {
			return ApiResponse.success(TestAuthResponse.from(currentUser, authentication));
		}

		@GetMapping("/api/admin/test")
		ApiResponse<TestAuthResponse> admin(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			Authentication authentication
		) {
			return ApiResponse.success(TestAuthResponse.from(currentUser, authentication));
		}
	}

	private record TestAuthResponse(
		Long userId,
		String role,
		List<String> authorities
	) {

		private static TestAuthResponse from(AuthenticatedUser currentUser, Authentication authentication) {
			List<String> authorities = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();
			return new TestAuthResponse(currentUser.userId(), currentUser.role(), authorities);
		}
	}
}
