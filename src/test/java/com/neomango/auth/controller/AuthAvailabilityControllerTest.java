package com.neomango.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.neomango.user.entity.User;
import com.neomango.user.repository.UserRepository;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AuthAvailabilityControllerTest {

	private static final String ENCODED_PASSWORD = "encoded-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
	}

	@Test
	void checkLoginIdReturnsAvailableWhenLoginIdIsUnused() throws Exception {
		mockMvc.perform(get("/api/auth/check-login-id")
				.param("loginId", "tester01"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("사용 가능한 아이디입니다."))
			.andExpect(jsonPath("$.data.available").value(true));
	}

	@Test
	void checkLoginIdReturnsUnavailableWhenLoginIdAlreadyExists() throws Exception {
		userRepository.saveAndFlush(user("tester01", "tester@example.com", "tester"));

		mockMvc.perform(get("/api/auth/check-login-id")
				.param("loginId", "tester01"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("이미 존재하는 아이디입니다."))
			.andExpect(jsonPath("$.data.available").value(false));
	}

	@Test
	void checkLoginIdReturnsUnavailableWhenLoginIdIsTooShort() throws Exception {
		assertInvalidLoginId("abc");
	}

	@Test
	void checkLoginIdReturnsUnavailableWhenLoginIdIsTooLong() throws Exception {
		assertInvalidLoginId("abcdefghijkl3");
	}

	@Test
	void checkLoginIdReturnsUnavailableWhenLoginIdContainsKorean() throws Exception {
		assertInvalidLoginId("tester한글");
	}

	@Test
	void checkLoginIdReturnsUnavailableWhenLoginIdContainsSpecialCharacter() throws Exception {
		assertInvalidLoginId("tester01!");
	}

	@Test
	void checkLoginIdReturnsUnavailableWhenLoginIdContainsWhitespace() throws Exception {
		assertInvalidLoginId("tester 01");
	}

	@Test
	void checkNicknameReturnsAvailableWhenNicknameIsUnused() throws Exception {
		mockMvc.perform(get("/api/auth/check-nickname")
				.param("nickname", "tester"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("사용 가능한 닉네임입니다."))
			.andExpect(jsonPath("$.data.available").value(true));
	}

	@Test
	void checkNicknameReturnsUnavailableWhenNicknameAlreadyExists() throws Exception {
		userRepository.saveAndFlush(user("tester01", "tester@example.com", "tester"));

		mockMvc.perform(get("/api/auth/check-nickname")
				.param("nickname", "tester"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("이미 존재하는 닉네임입니다."))
			.andExpect(jsonPath("$.data.available").value(false));
	}

	@Test
	void checkNicknameReturnsUnavailableWhenNicknameIsReservedAdministrator() throws Exception {
		assertReservedNickname("관리자");
	}

	@Test
	void checkNicknameReturnsUnavailableWhenNicknameIsReservedOperator() throws Exception {
		assertReservedNickname("운영자");
	}

	@Test
	void checkNicknameReturnsUnavailableWhenNicknameIsReservedAdminUppercase() throws Exception {
		assertReservedNickname("ADMIN");
	}

	@Test
	void checkNicknameReturnsUnavailableWhenNicknameIsReservedAdminMixedCase() throws Exception {
		assertReservedNickname("Admin");
	}

	@Test
	void checkNicknameReturnsUnavailableWhenNicknameIsTooShort() throws Exception {
		assertInvalidNicknameLength("a");
	}

	@Test
	void checkNicknameReturnsUnavailableWhenNicknameIsTooLong() throws Exception {
		assertInvalidNicknameLength("abcdefghijkl3");
	}

	private void assertInvalidLoginId(String loginId) throws Exception {
		mockMvc.perform(get("/api/auth/check-login-id")
				.param("loginId", loginId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("아이디는 4~12자의 영문 대소문자와 숫자만 사용할 수 있습니다."))
			.andExpect(jsonPath("$.data.available").value(false));
	}

	private void assertReservedNickname(String nickname) throws Exception {
		mockMvc.perform(get("/api/auth/check-nickname")
				.param("nickname", nickname))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("사용할 수 없는 닉네임입니다."))
			.andExpect(jsonPath("$.data.available").value(false));
	}

	private void assertInvalidNicknameLength(String nickname) throws Exception {
		mockMvc.perform(get("/api/auth/check-nickname")
				.param("nickname", nickname))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("닉네임은 2~12자여야 합니다."))
			.andExpect(jsonPath("$.data.available").value(false));
	}

	private User user(String loginId, String email, String nickname) {
		return User.create(loginId, email, ENCODED_PASSWORD, nickname);
	}
}
