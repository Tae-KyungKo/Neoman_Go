package com.neomango.team.entity;

import java.time.LocalDateTime;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "team_members",
	uniqueConstraints = @UniqueConstraint(name = "uk_team_members_team_user", columnNames = {"team_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMember {

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
	private TeamMemberRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TeamMemberStatus status;

	@Column(nullable = false)
	private LocalDateTime joinedAt;

	private TeamMember(Team team, User user, TeamMemberRole role) {
		this.team = team;
		this.user = user;
		this.role = role;
		this.status = TeamMemberStatus.ACTIVE;
		this.joinedAt = LocalDateTime.now();
	}

	public static TeamMember createOwner(Team team, User user) {
		return new TeamMember(team, user, TeamMemberRole.OWNER);
	}

	public static TeamMember createMember(Team team, User user) {
		return new TeamMember(team, user, TeamMemberRole.MEMBER);
	}
}
