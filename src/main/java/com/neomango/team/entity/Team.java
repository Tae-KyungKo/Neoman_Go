package com.neomango.team.entity;

import java.util.ArrayList;
import java.util.List;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.user.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

	@Column(nullable = false)
	private int capacity;

	@OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<TeamMember> members = new ArrayList<>();

	private Team(String name, int capacity) {
		this.name = name;
		this.capacity = capacity;
	}

	public static Team create(String name, int capacity, User owner) {
		Team team = new Team(name, capacity);
		team.addOwner(owner);
		return team;
	}

	private void addOwner(User owner) {
		this.members.add(TeamMember.createOwner(this, owner));
	}

	public void validateJoinable() {
		if (members.size() >= capacity) {
			throw new BusinessException(ErrorCode.TEAM_CAPACITY_EXCEEDED);
		}
	}
}

