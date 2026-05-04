package com.neomango.team.entity;

import java.time.LocalDateTime;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "team_applications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamApplication {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "team_id", nullable = false)
	private Team team;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TeamApplicationStatus status;

	@Column(length = 500)
	private String message;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	private LocalDateTime processedAt;

	private TeamApplication(Team team, User user, String message) {
		this.team = team;
		this.user = user;
		this.message = message;
		this.status = TeamApplicationStatus.PENDING;
		this.createdAt = LocalDateTime.now();
	}

	public static TeamApplication create(Team team, User user, String message) {
		return new TeamApplication(team, user, message);
	}

	public void approve() {
		validatePending();
		this.status = TeamApplicationStatus.APPROVED;
		this.processedAt = LocalDateTime.now();
	}

	public void reject() {
		validatePending();
		this.status = TeamApplicationStatus.REJECTED;
		this.processedAt = LocalDateTime.now();
	}

	public void validatePending() {
		if (status != TeamApplicationStatus.PENDING) {
			throw new BusinessException(ErrorCode.INVALID_TEAM_APPLICATION_STATUS);
		}
	}
}
