package com.neomango.admin.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;
import com.neomango.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

	private static final String EMAIL = "admin@test.com";
	private static final String LOGIN_ID = "admin001";
	private static final String RAW_PASSWORD = "strong-password";
	private static final String ENCODED_PASSWORD = "encoded-admin-password";
	private static final String NICKNAME = "mangoManager";

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private AdminBootstrapService adminBootstrapService;

	@Test
	void bootstrapCreatesAdminWhenLoginIdEmailAndNicknameAreUnusedAndNoAdminExists() {
		when(userRepository.findByLoginId(LOGIN_ID)).thenReturn(Optional.empty());
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
		when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
		when(userRepository.existsByNickname(NICKNAME)).thenReturn(false);
		when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

		AdminBootstrapResult result = adminBootstrapService.bootstrap(properties());

		assertThat(result.adminCreated()).isTrue();
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User savedUser = userCaptor.getValue();
		assertThat(savedUser.getLoginId()).isEqualTo(LOGIN_ID);
		assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
		assertThat(savedUser.getNickname()).isEqualTo(NICKNAME);
		assertThat(savedUser.getRole()).isEqualTo(UserRole.ADMIN);
		assertThat(savedUser.getStatus()).isEqualTo(com.neomango.user.entity.UserStatus.ACTIVE);
		assertThat(savedUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
		assertThat(savedUser.getPassword()).isNotEqualTo(RAW_PASSWORD);
	}

	@Test
	void bootstrapSkipsWhenLoginIdAlreadyBelongsToAdmin() {
		User existingAdmin = User.createAdmin(LOGIN_ID, EMAIL, ENCODED_PASSWORD, NICKNAME);
		when(userRepository.findByLoginId(LOGIN_ID)).thenReturn(Optional.of(existingAdmin));

		AdminBootstrapResult result = adminBootstrapService.bootstrap(properties());

		assertThat(result.adminCreated()).isFalse();
		assertThat(result.skipReason()).isEqualTo(AdminBootstrapSkipReason.LOGIN_ID_ALREADY_ADMIN);
		verify(userRepository, never()).save(any());
	}

	@Test
	void bootstrapFailsWhenLoginIdAlreadyBelongsToUser() {
		User existingUser = User.create(LOGIN_ID, EMAIL, ENCODED_PASSWORD, NICKNAME);
		when(userRepository.findByLoginId(LOGIN_ID)).thenReturn(Optional.of(existingUser));

		assertThatThrownBy(() -> adminBootstrapService.bootstrap(properties()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("loginId")
			.hasMessageContaining("non-admin user");

		verify(userRepository, never()).save(any());
	}

	@Test
	void bootstrapSkipsWhenEmailAlreadyBelongsToAdmin() {
		User existingAdmin = User.createAdmin(LOGIN_ID, EMAIL, ENCODED_PASSWORD, NICKNAME);
		when(userRepository.findByLoginId(LOGIN_ID)).thenReturn(Optional.empty());
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingAdmin));

		AdminBootstrapResult result = adminBootstrapService.bootstrap(properties());

		assertThat(result.adminCreated()).isFalse();
		assertThat(result.skipReason()).isEqualTo(AdminBootstrapSkipReason.EMAIL_ALREADY_ADMIN);
		verify(userRepository, never()).save(any());
	}

	@Test
	void bootstrapFailsWhenEmailAlreadyBelongsToUser() {
		User existingUser = User.create(com.neomango.support.TestLoginIds.next(), EMAIL, ENCODED_PASSWORD, NICKNAME);
		when(userRepository.findByLoginId(LOGIN_ID)).thenReturn(Optional.empty());
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));

		assertThatThrownBy(() -> adminBootstrapService.bootstrap(properties()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("non-admin user");

		verify(userRepository, never()).save(any());
	}

	@Test
	void bootstrapSkipsWhenAnotherAdminAlreadyExists() {
		when(userRepository.findByLoginId(LOGIN_ID)).thenReturn(Optional.empty());
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
		when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

		AdminBootstrapResult result = adminBootstrapService.bootstrap(properties());

		assertThat(result.adminCreated()).isFalse();
		assertThat(result.skipReason()).isEqualTo(AdminBootstrapSkipReason.ADMIN_ALREADY_EXISTS);
		verify(userRepository, never()).save(any());
	}

	@Test
	void bootstrapFailsWhenNicknameAlreadyExists() {
		when(userRepository.findByLoginId(LOGIN_ID)).thenReturn(Optional.empty());
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
		when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
		when(userRepository.existsByNickname(NICKNAME)).thenReturn(true);

		assertThatThrownBy(() -> adminBootstrapService.bootstrap(properties()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("nickname");

		verify(userRepository, never()).save(any());
	}

	private AdminBootstrapProperties properties() {
		return new AdminBootstrapProperties(true, LOGIN_ID, EMAIL, RAW_PASSWORD, NICKNAME);
	}
}
