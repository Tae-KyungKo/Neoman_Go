package com.neomango.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neomango.global.exception.ErrorCode;

class CustomAccessDeniedHandlerTest {

	private final CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler(new ObjectMapper());

	@Test
	void accessDeniedHandlerReturnsErrorResponse() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.handle(
			new MockHttpServletRequest(),
			response,
			new AccessDeniedException("denied")
		);

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentAsString()).contains("\"status\":403");
		assertThat(response.getContentAsString()).contains("\"code\":\"" + ErrorCode.FORBIDDEN.getCode() + "\"");
	}
}
