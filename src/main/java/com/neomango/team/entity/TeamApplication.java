package com.neomango.team.entity;

import java.time.LocalDateTime;

import com.neomango.team.exception.ApplicationAlreadyProcessedException;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
	@JoinColumn(name = "applicant_id", nullable = false)
	private User applicant;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TeamApplicationStatus status;

	@Column(length = 500)
	private String message;

	@Column(nullable = false)
	private boolean active;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	private LocalDateTime canceledAt;

	private TeamApplication(Team team, User applicant, String message) {
		this.team = team;
		this.applicant = applicant;
		this.status = TeamApplicationStatus.PENDING;
		this.message = message;
		this.active = true;
	}

	public static TeamApplication create(Team team, User applicant, String message) {
		return new TeamApplication(team, applicant, message);
	}

	public void cancel() {
		validatePending();
		this.status = TeamApplicationStatus.CANCELED;
		this.active = false;
		this.canceledAt = LocalDateTime.now();
	}

	public void approve() {
		validatePending();
		this.status = TeamApplicationStatus.APPROVED;
		this.active = false;
	}

	public void reject() {
		validatePending();
		this.status = TeamApplicationStatus.REJECTED;
		this.active = false;
	}

	public void validatePending() {
		if (this.status != TeamApplicationStatus.PENDING) {
			throw new ApplicationAlreadyProcessedException();
		}
	}

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
