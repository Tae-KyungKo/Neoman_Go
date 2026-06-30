package com.neomango.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class SignupRequestValidationTest {

	private static final String VALID_LOGIN_ID = "tester01";
	private static final String VALID_PASSWORD = "Password123!";
	private static final String VALID_EMAIL = "tester@example.com";
	private static final String VALID_NICKNAME = "tester";

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void validSignupRequestPassesValidation() {
		SignupRequest request = request(VALID_LOGIN_ID, VALID_PASSWORD, VALID_PASSWORD, VALID_EMAIL, VALID_NICKNAME);

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void loginIdIsRequired() {
		SignupRequest request = request(null, VALID_PASSWORD, VALID_PASSWORD, VALID_EMAIL, VALID_NICKNAME);

		assertThat(fieldsOf(validator.validate(request))).contains("loginId");
	}

	@Test
	void loginIdMustHaveAtLeastFourCharacters() {
		SignupRequest request = request("abc", VALID_PASSWORD, VALID_PASSWORD, VALID_EMAIL, VALID_NICKNAME);

		assertThat(fieldsOf(validator.validate(request))).contains("loginId");
	}

	@Test
	void loginIdMustHaveAtMostTwelveCharacters() {
		SignupRequest request = request("abcdefghijkl3", VALID_PASSWORD, VALID_PASSWORD, VALID_EMAIL, VALID_NICKNAME);

		assertThat(fieldsOf(validator.validate(request))).contains("loginId");
	}

	@Test
	void loginIdRejectsKoreanCharacters() {
		SignupRequest request = request("tester한글", VALID_PASSWORD, VALID_PASSWORD, VALID_EMAIL, VALID_NICKNAME);

		assertThat(fieldsOf(validator.validate(request))).contains("loginId");
	}

	@Test
	void loginIdRejectsSpecialCharacters() {
		SignupRequest request = request("tester01!", VALID_PASSWORD, VALID_PASSWORD, VALID_EMAIL, VALID_NICKNAME);

		assertThat(fieldsOf(validator.validate(request))).contains("loginId");
	}

	@Test
	void loginIdRejectsWhitespace() {
		SignupRequest request = request("tester 01", VALID_PASSWORD, VALID_PASSWORD, VALID_EMAIL, VALID_NICKNAME);

		assertThat(fieldsOf(validator.validate(request))).contains("loginId");
	}

	@Test
	void passwordMustHaveAtLeastEightCharacters() {
		SignupRequest request = request(VALID_LOGIN_ID, "Abc123!", "Abc123!", VALID_EMAIL, VALID_NICKNAME);

		assertThat(fieldsOf(validator.validate(request))).contains("password");
	}

	@Test
	void passwordMustHaveAtMostSixteenCharacters() {
		SignupRequest request = request(VALID_LOGIN_ID, "Password1234567!!", "Password1234567!!", VALID_EMAIL, VALID_NICKNAME);

		assertThat(fieldsOf(validator.validate(request))).contains("password");
	}

	@Test
	void passwordRejectsKoreanCharacters() {
		SignupRequest request = request(VALID_LOGIN_ID, "비밀번호123!", "비밀번호123!", VALID_EMAIL, VALID_NICKNAME);

		assertThat(fieldsOf(validator.validate(request))).contains("password");
	}

	@Test
	void passwordRejectsWhitespace() {
		SignupRequest request = request(VALID_LOGIN_ID, "Password 123!", "Password 123!", VALID_EMAIL, VALID_NICKNAME);

		assertThat(fieldsOf(validator.validate(request))).contains("password");
	}

	@Test
	void passwordConfirmIsRequired() {
		SignupRequest request = request(VALID_LOGIN_ID, VALID_PASSWORD, null, VALID_EMAIL, VALID_NICKNAME);

		assertThat(fieldsOf(validator.validate(request))).contains("passwordConfirm");
	}

	@Test
	void nicknameMustHaveAtLeastTwoCharacters() {
		SignupRequest request = request(VALID_LOGIN_ID, VALID_PASSWORD, VALID_PASSWORD, VALID_EMAIL, "a");

		assertThat(fieldsOf(validator.validate(request))).contains("nickname");
	}

	@Test
	void nicknameMustHaveAtMostTwelveCharacters() {
		SignupRequest request = request(VALID_LOGIN_ID, VALID_PASSWORD, VALID_PASSWORD, VALID_EMAIL, "abcdefghijkl3");

		assertThat(fieldsOf(validator.validate(request))).contains("nickname");
	}

	private SignupRequest request(
		String loginId,
		String password,
		String passwordConfirm,
		String email,
		String nickname
	) {
		return new SignupRequest(loginId, password, passwordConfirm, email, nickname);
	}

	private Set<String> fieldsOf(Set<ConstraintViolation<SignupRequest>> violations) {
		return violations.stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(java.util.stream.Collectors.toSet());
	}
}
