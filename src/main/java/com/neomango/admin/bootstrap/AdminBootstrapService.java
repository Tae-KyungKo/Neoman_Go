package com.neomango.admin.bootstrap;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;
import com.neomango.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminBootstrapService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public AdminBootstrapResult bootstrap(AdminBootstrapProperties properties) {
		Optional<User> userByLoginId = userRepository.findByLoginId(properties.loginId());
		Optional<User> userByEmail = userRepository.findByEmail(properties.email());

		if (userByLoginId.isPresent() && userByLoginId.get().getRole() != UserRole.ADMIN) {
			throw new IllegalStateException("ADMIN bootstrap loginId is already used by a non-admin user.");
		}

		if (userByEmail.isPresent() && userByEmail.get().getRole() != UserRole.ADMIN) {
			throw new IllegalStateException("ADMIN bootstrap email is already used by a non-admin user.");
		}

		if (userByLoginId.isPresent()) {
			return AdminBootstrapResult.skipped(AdminBootstrapSkipReason.LOGIN_ID_ALREADY_ADMIN);
		}

		if (userByEmail.isPresent()) {
			return AdminBootstrapResult.skipped(AdminBootstrapSkipReason.EMAIL_ALREADY_ADMIN);
		}

		return createAdminWhenNoAdminExists(properties);
	}

	private AdminBootstrapResult createAdminWhenNoAdminExists(AdminBootstrapProperties properties) {
		if (userRepository.existsByRole(UserRole.ADMIN)) {
			return AdminBootstrapResult.skipped(AdminBootstrapSkipReason.ADMIN_ALREADY_EXISTS);
		}

		if (userRepository.existsByNickname(properties.nickname())) {
			throw new IllegalStateException("ADMIN bootstrap nickname is already used.");
		}

		User admin = User.createAdmin(
			properties.loginId(),
			properties.email(),
			passwordEncoder.encode(properties.password()),
			properties.nickname()
		);
		userRepository.save(admin);
		return AdminBootstrapResult.created();
	}
}
