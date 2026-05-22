package com.neomango.team.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.neomango.user.entity.User;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "teams")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(nullable = false, length = 50)
	private String category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TeamStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	private LocalDateTime deletedAt;

	@OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<TeamMember> members = new ArrayList<>();

	private Team(String name, String description, String category, User createdBy) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.status = TeamStatus.RECRUITING;
		this.createdBy = createdBy;
	}

	public static Team create(
		String name,
		String description,
		String category,
		User createdBy
	) {
		Team team = new Team(name, description, category, createdBy);
		team.addMember(TeamMember.createOwner(team, createdBy));
		return team;
	}

	public void addMember(TeamMember teamMember) {
		this.members.add(teamMember);
	}

	public void close() {
		this.status = TeamStatus.CLOSED;
	}

	public void softDelete() {
		this.status = TeamStatus.DELETED;
		this.deletedAt = LocalDateTime.now();
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
