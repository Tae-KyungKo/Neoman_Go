package com.neomango.team.dto;

import java.time.LocalDateTime;

import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamApplicationStatus;

public record TeamApplicationSummaryResponse(
	Long applicationId,
	Long teamId,
	String teamName,
	String category,
	TeamApplicationStatus status,
	String message,
	LocalDateTime createdAt
) {

	public static TeamApplicationSummaryResponse from(TeamApplication teamApplication) {
		Team team = teamApplication.getTeam();
		return new TeamApplicationSummaryResponse(
			teamApplication.getId(),
			team.getId(),
			team.getName(),
			team.getCategory(),
			teamApplication.getStatus(),
			teamApplication.getMessage(),
			teamApplication.getCreatedAt()
		);
	}
}
