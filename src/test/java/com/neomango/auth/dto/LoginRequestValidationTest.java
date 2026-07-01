package com.neomango.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class LoginRequestValidationTest {

	private static final String VALID_LOGIN_ID = "tester01";
	private static final String VALID_PASSWORD = "Password123!";

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void validLoginRequestPassesValidation() {
		LoginRequest request = new LoginRequest(VALID_LOGIN_ID, VALID_PASSWORD);

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void loginIdIsRequired() {
		LoginRequest request = new LoginRequest(null, VALID_PASSWORD);

		assertThat(fieldsOf(validator.validate(request))).contains("loginId");
	}

	@Test
	void loginIdMustHaveAtLeastFourCharacters() {
		LoginRequest request = new LoginRequest("abc", VALID_PASSWORD);

		assertThat(fieldsOf(validator.validate(request))).contains("loginId");
	}

	@Test
	void loginIdMustHaveAtMostTwelveCharacters() {
		LoginRequest request = new LoginRequest("abcdefghijkl3", VALID_PASSWORD);

		assertThat(fieldsOf(validator.validate(request))).contains("loginId");
	}

	@Test
	void loginIdRejectsKoreanCharacters() {
		LoginRequest request = new LoginRequest("tester한글", VALID_PASSWORD);

		assertThat(fieldsOf(validator.validate(request))).contains("loginId");
	}

	@Test
	void loginIdRejectsSpecialCharacters() {
		LoginRequest request = new LoginRequest("tester01!", VALID_PASSWORD);

		assertThat(fieldsOf(validator.validate(request))).contains("loginId");
	}

	@Test
	void loginIdRejectsWhitespace() {
		LoginRequest request = new LoginRequest("tester 01", VALID_PASSWORD);

		assertThat(fieldsOf(validator.validate(request))).contains("loginId");
	}

	@Test
	void passwordIsRequired() {
		LoginRequest request = new LoginRequest(VALID_LOGIN_ID, null);

		assertThat(fieldsOf(validator.validate(request))).contains("password");
	}

	private Set<String> fieldsOf(Set<ConstraintViolation<LoginRequest>> violations) {
		return violations.stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(java.util.stream.Collectors.toSet());
	}
}
