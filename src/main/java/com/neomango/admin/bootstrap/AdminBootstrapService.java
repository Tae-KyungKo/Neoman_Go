package com.neomango.admin.bootstrap;

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
		return userRepository.findByEmail(properties.email())
			.map(this::handleExistingEmail)
			.orElseGet(() -> createAdminWhenNoAdminExists(properties));
	}

	private AdminBootstrapResult handleExistingEmail(User user) {
		if (user.getRole() == UserRole.ADMIN) {
			return AdminBootstrapResult.skipped(AdminBootstrapSkipReason.EMAIL_ALREADY_ADMIN);
		}

		throw new IllegalStateException("ADMIN bootstrap email is already used by a non-admin user.");
	}

	private AdminBootstrapResult createAdminWhenNoAdminExists(AdminBootstrapProperties properties) {
		if (userRepository.existsByRole(UserRole.ADMIN)) {
			return AdminBootstrapResult.skipped(AdminBootstrapSkipReason.ADMIN_ALREADY_EXISTS);
		}

		User admin = User.createAdmin(
			properties.email(),
			passwordEncoder.encode(properties.password()),
			properties.nickname()
		);
		userRepository.save(admin);
		return AdminBootstrapResult.created();
	}
}
