package com.neomango.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.constraints.Min;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neomango.auth.jwt.JwtTokenProvider;
import com.neomango.user.entity.UserRole;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Import(GlobalExceptionHandlerTest.TestExceptionController.class)
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Test
	@WithMockUser
	void businessExceptionReturnsErrorResponse() throws Exception {
		mockMvc.perform(get("/api/test/errors/business"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_EMAIL.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_EMAIL.getMessage()));
	}

	@Test
	void validationExceptionReturnsFieldErrors() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType("application/json")
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
			.andExpect(jsonPath("$.errors").isArray())
			.andExpect(jsonPath("$.errors[0].field").exists())
			.andExpect(jsonPath("$.errors[0].message").exists());
	}

	@Test
	@WithMockUser
	void constraintViolationExceptionReturnsFieldErrors() throws Exception {
		mockMvc.perform(get("/api/test/errors/constraint")
				.param("page", "0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
			.andExpect(jsonPath("$.errors").isArray())
			.andExpect(jsonPath("$.errors[0].field").exists())
			.andExpect(jsonPath("$.errors[0].message").exists());
	}

	@Test
	void unauthenticatedRequestReturnsErrorResponse() throws Exception {
		mockMvc.perform(get("/api/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()));
	}

	@Test
	void invalidJwtReturnsErrorResponse() throws Exception {
		mockMvc.perform(get("/api/users/me")
				.header("Authorization", "Bearer invalid-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()));
	}

	@Test
	void missingUserForAuthenticatedRequestReturnsUnauthorizedErrorResponse() throws Exception {
		String accessToken = jwtTokenProvider.createAccessToken(999999L, UserRole.USER);

		mockMvc.perform(get("/api/users/me")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()));
	}

	@RestController
	@Validated
	static class TestExceptionController {

		@GetMapping("/api/test/errors/business")
		void businessException() {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		@GetMapping("/api/test/errors/constraint")
		void constraintViolation(@RequestParam @Min(1) int page) {
		}
	}
}
