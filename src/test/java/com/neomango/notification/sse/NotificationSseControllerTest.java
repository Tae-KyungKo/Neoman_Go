package com.neomango.notification.sse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.neomango.auth.jwt.JwtTokenProvider;
import com.neomango.notification.repository.NotificationRepository;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;
import com.neomango.user.repository.UserRepository;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class NotificationSseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private NotificationSseService notificationSseService;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		deleteAll();
	}

	@AfterEach
	void tearDown() {
		deleteAll();
	}

	@Test
	void streamAllowsAuthenticatedUser() throws Exception {
		User user = userRepository.save(User.create("sse-user@test.com", "encoded-password", "sseUser"));

		mockMvc.perform(get("/api/notifications/stream")
				.header("Authorization", "Bearer " + accessToken(user))
				.accept(MediaType.TEXT_EVENT_STREAM))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
			.andExpect(request().asyncStarted());
	}

	@Test
	void streamRejectsUnauthenticatedUser() throws Exception {
		mockMvc.perform(get("/api/notifications/stream")
				.accept(MediaType.TEXT_EVENT_STREAM))
			.andExpect(status().isUnauthorized());
	}

	private void deleteAll() {
		notificationSseService.clear();
		notificationRepository.deleteAll();
		userRepository.deleteAll();
	}

	private String accessToken(User user) {
		return jwtTokenProvider.createAccessToken(user.getId(), UserRole.USER);
	}
}
