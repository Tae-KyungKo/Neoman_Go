package com.neomango.user.entity;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.neomango.user.policy.UserPolicy;

@Getter
@Entity
@Table(
	name = "users",
	uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	private static final AtomicLong LEGACY_LOGIN_ID_SEQUENCE = new AtomicLong(1);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "login_id", nullable = false, length = UserPolicy.LOGIN_ID_MAX_LENGTH, unique = true)
	private String loginId;

	@Column(nullable = false, length = 100)
	private String email;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false, length = 30)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserStatus status;

	private LocalDateTime deletedAt;

	private User(String loginId, String email, String password, String nickname, UserRole role) {
		this.loginId = loginId;
		this.email = email;
		this.password = password;
		this.nickname = nickname;
		this.role = role;
		this.status = UserStatus.ACTIVE;
	}

	public static User create(String loginId, String email, String encodedPassword, String nickname) {
		return new User(loginId, email, encodedPassword, nickname, UserRole.USER);
	}

	public static User createAdmin(String loginId, String email, String encodedPassword, String nickname) {
		return new User(loginId, email, encodedPassword, nickname, UserRole.ADMIN);
	}

	@Deprecated
	public static User create(String email, String encodedPassword, String nickname) {
		// TODO(Phase 9-5): Remove this legacy factory after signup accepts loginId.
		return new User(nextLegacyLoginId(), email, encodedPassword, nickname, UserRole.USER);
	}

	@Deprecated
	public static User createAdmin(String email, String encodedPassword, String nickname) {
		// TODO(Phase 9-9): Remove this legacy admin factory after admin bootstrap accepts loginId.
		return new User(nextLegacyLoginId(), email, encodedPassword, nickname, UserRole.ADMIN);
	}

	private static String nextLegacyLoginId() {
		return "legacy" + String.format("%06d", LEGACY_LOGIN_ID_SEQUENCE.getAndIncrement());
	}

	public void softDelete() {
		this.status = UserStatus.DELETED;
		this.deletedAt = LocalDateTime.now();
	}
}

