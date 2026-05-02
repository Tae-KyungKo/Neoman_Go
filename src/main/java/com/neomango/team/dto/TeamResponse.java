package com.neomango.team.dto;

import com.neomango.team.entity.Team;

public record TeamResponse(
	Long id,
	String name,
	int capacity,
	int memberCount
) {

	public static TeamResponse from(Team team) {
		return new TeamResponse(
			team.getId(),
			team.getName(),
			team.getCapacity(),
			team.getMembers().size()
		);
	}
}

