package com.neomango.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.neomango.user.entity.User;

@ActiveProfiles("test")
@DataJpaTest
class UserRepositoryTest {

	private static final String ENCODED_PASSWORD = "encoded-password";

	@Autowired
	private UserRepository userRepository;

	@Test
	void findByLoginIdReturnsUserWhenLoginIdExists() {
		User user = userRepository.saveAndFlush(user("tester01", "tester@example.com", "tester"));

		assertThat(userRepository.findByLoginId("tester01"))
			.isPresent()
			.get()
			.extracting(User::getId)
			.isEqualTo(user.getId());
	}

	@Test
	void findByLoginIdReturnsEmptyWhenLoginIdDoesNotExist() {
		userRepository.saveAndFlush(user("tester01", "tester@example.com", "tester"));

		assertThat(userRepository.findByLoginId("missing01")).isEmpty();
	}

	@Test
	void existsByLoginIdReturnsTrueWhenLoginIdExists() {
		userRepository.saveAndFlush(user("tester01", "tester@example.com", "tester"));

		assertThat(userRepository.existsByLoginId("tester01")).isTrue();
	}

	@Test
	void existsByLoginIdReturnsFalseWhenLoginIdDoesNotExist() {
		userRepository.saveAndFlush(user("tester01", "tester@example.com", "tester"));

		assertThat(userRepository.existsByLoginId("missing01")).isFalse();
	}

	@Test
	void existsByEmailReturnsTrueWhenEmailExists() {
		userRepository.saveAndFlush(user("tester01", "tester@example.com", "tester"));

		assertThat(userRepository.existsByEmail("tester@example.com")).isTrue();
	}

	@Test
	void existsByEmailReturnsFalseWhenEmailDoesNotExist() {
		userRepository.saveAndFlush(user("tester01", "tester@example.com", "tester"));

		assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
	}

	@Test
	void existsByNicknameReturnsTrueWhenNicknameExists() {
		userRepository.saveAndFlush(user("tester01", "tester@example.com", "tester"));

		assertThat(userRepository.existsByNickname("tester")).isTrue();
	}

	@Test
	void existsByNicknameReturnsFalseWhenNicknameDoesNotExist() {
		userRepository.saveAndFlush(user("tester01", "tester@example.com", "tester"));

		assertThat(userRepository.existsByNickname("missing")).isFalse();
	}

	@Test
	void existsByLoginIdIsCaseSensitive() {
		userRepository.saveAndFlush(user("Tester01", "tester@example.com", "tester"));

		assertThat(userRepository.existsByLoginId("tester01")).isFalse();
	}

	@Test
	void existsByNicknameIsCaseSensitive() {
		userRepository.saveAndFlush(user("tester01", "tester@example.com", "Tester"));

		assertThat(userRepository.existsByNickname("tester")).isFalse();
	}

	private User user(String loginId, String email, String nickname) {
		return User.create(loginId, email, ENCODED_PASSWORD, nickname);
	}
}
