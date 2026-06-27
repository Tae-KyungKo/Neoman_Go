package com.neomango.team.dto;

import java.time.LocalDateTime;

import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamStatus;
import com.neomango.user.entity.User;

public record TeamSummaryResponse(
	Long id,
	String name,
	String category,
	TeamStatus status,
	Long ownerId,
	String ownerNickname,
	LocalDateTime createdAt
) {

	public static TeamSummaryResponse from(Team team, TeamMember ownerMember) {
		User owner = ownerMember.getUser();
		return new TeamSummaryResponse(
			team.getId(),
			team.getName(),
			team.getCategory(),
			team.getStatus(),
			owner.getId(),
			owner.getNickname(),
			team.getCreatedAt()
		);
	}
}
