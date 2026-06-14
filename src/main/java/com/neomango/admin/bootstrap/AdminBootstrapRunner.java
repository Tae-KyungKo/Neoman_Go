package com.neomango.admin.bootstrap;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.neomango.auth.dto.SignupRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

	private final AdminBootstrapProperties properties;
	private final AdminBootstrapService adminBootstrapService;
	private final Validator validator;

	@Override
	public void run(ApplicationArguments args) {
		if (!properties.enabled()) {
			log.info("ADMIN bootstrap is disabled.");
			return;
		}

		log.info("ADMIN bootstrap is enabled. email={}", maskEmail(properties.email()));
		try {
			validate(properties);

			AdminBootstrapResult result = adminBootstrapService.bootstrap(properties);
			if (result.adminCreated()) {
				log.info("ADMIN bootstrap created the initial ADMIN account. email={}", maskEmail(properties.email()));
				return;
			}

			log.info("ADMIN bootstrap skipped. reason={}, email={}", result.skipReason(), maskEmail(properties.email()));
		} catch (RuntimeException exception) {
			log.error("ADMIN bootstrap failed. email={}", maskEmail(properties.email()));
			throw exception;
		}
	}

	private void validate(AdminBootstrapProperties properties) {
		if (!StringUtils.hasText(properties.email())
			|| !StringUtils.hasText(properties.password())
			|| !StringUtils.hasText(properties.nickname())) {
			throw new IllegalStateException("ADMIN bootstrap requires ADMIN_EMAIL, ADMIN_PASSWORD, and ADMIN_NICKNAME.");
		}

		SignupRequest request = new SignupRequest(properties.email(), properties.password(), properties.nickname());
		Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
		if (!violations.isEmpty()) {
			String invalidFields = violations.stream()
				.map(violation -> violation.getPropertyPath().toString())
				.sorted()
				.distinct()
				.collect(Collectors.joining(", "));
			throw new IllegalStateException("Invalid ADMIN bootstrap configuration fields: " + invalidFields);
		}
	}

	private String maskEmail(String email) {
		if (!StringUtils.hasText(email)) {
			return "<empty>";
		}

		int atIndex = email.indexOf('@');
		if (atIndex <= 0) {
			return "<invalid>";
		}

		String localPart = email.substring(0, atIndex);
		String domain = email.substring(atIndex);
		String visiblePrefix = localPart.substring(0, Math.min(2, localPart.length()));
		return visiblePrefix + "***" + domain;
	}
}
