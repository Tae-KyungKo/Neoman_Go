package com.neomango.admin.bootstrap;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.neomango.user.policy.UserPolicy;

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

		log.info("ADMIN bootstrap is enabled. loginId={}, email={}",
			maskLoginId(properties.loginId()), maskEmail(properties.email()));
		try {
			validate(properties);

			AdminBootstrapResult result = adminBootstrapService.bootstrap(properties);
			if (result.adminCreated()) {
				log.info("ADMIN bootstrap created the initial ADMIN account. loginId={}, email={}",
					maskLoginId(properties.loginId()), maskEmail(properties.email()));
				return;
			}

			log.info("ADMIN bootstrap skipped. reason={}, loginId={}, email={}",
				result.skipReason(), maskLoginId(properties.loginId()), maskEmail(properties.email()));
		} catch (RuntimeException exception) {
			log.error("ADMIN bootstrap failed. loginId={}, email={}",
				maskLoginId(properties.loginId()), maskEmail(properties.email()));
			throw exception;
		}
	}

	private void validate(AdminBootstrapProperties properties) {
		String missingFields = missingRequiredFields(properties);
		if (StringUtils.hasText(missingFields)) {
			throw new IllegalStateException("ADMIN bootstrap requires " + missingFields + ".");
		}

		AdminBootstrapValidationRequest request = new AdminBootstrapValidationRequest(
			properties.loginId(),
			properties.email(),
			properties.password(),
			properties.nickname()
		);
		Set<ConstraintViolation<AdminBootstrapValidationRequest>> violations = validator.validate(request);
		if (!violations.isEmpty()) {
			String invalidFields = violations.stream()
				.map(violation -> violation.getPropertyPath().toString())
				.sorted()
				.distinct()
				.collect(Collectors.joining(", "));
			throw new IllegalStateException("Invalid ADMIN bootstrap configuration fields: " + invalidFields);
		}

		if (UserPolicy.isReservedNickname(properties.nickname())) {
			throw new IllegalStateException("Invalid ADMIN bootstrap configuration fields: nickname");
		}
	}

	private String missingRequiredFields(AdminBootstrapProperties properties) {
		return Set.of(
				new RequiredField("ADMIN_BOOTSTRAP_LOGIN_ID", properties.loginId()),
				new RequiredField("ADMIN_BOOTSTRAP_EMAIL", properties.email()),
				new RequiredField("ADMIN_BOOTSTRAP_PASSWORD", properties.password()),
				new RequiredField("ADMIN_BOOTSTRAP_NICKNAME", properties.nickname())
			)
			.stream()
			.filter(field -> !StringUtils.hasText(field.value()))
			.map(RequiredField::name)
			.sorted()
			.collect(Collectors.joining(", "));
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

	private String maskLoginId(String loginId) {
		if (!StringUtils.hasText(loginId)) {
			return "<empty>";
		}

		String visiblePrefix = loginId.substring(0, Math.min(2, loginId.length()));
		return visiblePrefix + "***";
	}

	private record RequiredField(
		String name,
		String value
	) {
	}

	private record AdminBootstrapValidationRequest(
		@NotBlank
		@Pattern(regexp = UserPolicy.LOGIN_ID_PATTERN)
		String loginId,

		@NotBlank
		@Email
		String email,

		@NotBlank
		String password,

		@NotBlank
		@Size(min = UserPolicy.NICKNAME_MIN_LENGTH, max = UserPolicy.NICKNAME_MAX_LENGTH)
		String nickname
	) {
	}
}
