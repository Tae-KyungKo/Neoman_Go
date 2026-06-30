package com.neomango.user.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserPolicyTest {

	@Test
	void loginIdPatternAllowsValidLengthBoundaries() {
		assertThat(UserPolicy.LOGIN_ID_REGEX.matcher("abcd").matches()).isTrue();
		assertThat(UserPolicy.LOGIN_ID_REGEX.matcher("abcdefghijkl").matches()).isTrue();
	}

	@Test
	void loginIdPatternRejectsInvalidLengthBoundaries() {
		assertThat(UserPolicy.LOGIN_ID_REGEX.matcher("abc").matches()).isFalse();
		assertThat(UserPolicy.LOGIN_ID_REGEX.matcher("abcdefghijklm").matches()).isFalse();
	}

	@Test
	void loginIdPatternAllowsKoreanAndNumber() {
		assertThat(UserPolicy.LOGIN_ID_REGEX.matcher("가나다라").matches()).isTrue();
		assertThat(UserPolicy.LOGIN_ID_REGEX.matcher("user1234").matches()).isTrue();
	}

	@Test
	void loginIdPatternRejectsSpecialCharacterAndWhitespace() {
		assertThat(UserPolicy.LOGIN_ID_REGEX.matcher("user!").matches()).isFalse();
		assertThat(UserPolicy.LOGIN_ID_REGEX.matcher("user 1").matches()).isFalse();
	}

	@Test
	void passwordPatternAllowsValidLengthBoundaries() {
		assertThat(UserPolicy.PASSWORD_REGEX.matcher("abcd1234").matches()).isTrue();
		assertThat(UserPolicy.PASSWORD_REGEX.matcher("abcd1234!@#$%^&*").matches()).isTrue();
	}

	@Test
	void passwordPatternRejectsInvalidLengthBoundaries() {
		assertThat(UserPolicy.PASSWORD_REGEX.matcher("abc123!").matches()).isFalse();
		assertThat(UserPolicy.PASSWORD_REGEX.matcher("abcd1234!@#$%^&*x").matches()).isFalse();
	}

	@Test
	void passwordPatternAllowsLettersNumbersAndCommonSpecialCharacters() {
		assertThat(UserPolicy.PASSWORD_REGEX.matcher("Aa1234!@").matches()).isTrue();
	}

	@Test
	void passwordPatternRejectsKoreanAndWhitespace() {
		assertThat(UserPolicy.PASSWORD_REGEX.matcher("비밀번호123!").matches()).isFalse();
		assertThat(UserPolicy.PASSWORD_REGEX.matcher("abc 123!").matches()).isFalse();
	}

	@Test
	void reservedNicknameReturnsTrueForReservedNames() {
		assertThat(UserPolicy.isReservedNickname("관리자")).isTrue();
		assertThat(UserPolicy.isReservedNickname("운영자")).isTrue();
		assertThat(UserPolicy.isReservedNickname("ADMIN")).isTrue();
		assertThat(UserPolicy.isReservedNickname("admin")).isTrue();
		assertThat(UserPolicy.isReservedNickname("Admin")).isTrue();
	}

	@Test
	void reservedNicknameReturnsFalseForNullAndNonReservedName() {
		assertThat(UserPolicy.isReservedNickname(null)).isFalse();
		assertThat(UserPolicy.isReservedNickname(" tester ")).isFalse();
	}
}
