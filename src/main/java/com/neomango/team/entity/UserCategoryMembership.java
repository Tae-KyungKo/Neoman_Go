package com.neomango.team.entity;

import java.time.LocalDateTime;

import com.neomango.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "user_category_memberships",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_user_category_memberships_user_category",
		columnNames = {"user_id", "category"}
	)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCategoryMembership {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 50)
	private String category;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "team_id", nullable = false)
	private Team team;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	private UserCategoryMembership(User user, String category, Team team) {
		this.user = user;
		this.category = category;
		this.team = team;
	}

	public static UserCategoryMembership create(User user, String category, Team team) {
		return new UserCategoryMembership(user, category, team);
	}

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}
}
