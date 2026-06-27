package com.neomango.team.dto;

import java.time.LocalDateTime;

import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamStatus;
import com.neomango.user.entity.User;

public record TeamResponse(
	Long id,
	String name,
	String description,
	String category,
	TeamStatus status,
	Long ownerId,
	String ownerNickname,
	LocalDateTime createdAt
) {

	public static TeamResponse from(Team team) {
		User owner = team.getCreatedBy();
		return new TeamResponse(
			team.getId(),
			team.getName(),
			team.getDescription(),
			team.getCategory(),
			team.getStatus(),
			owner.getId(),
			owner.getNickname(),
			team.getCreatedAt()
		);
	}
}
