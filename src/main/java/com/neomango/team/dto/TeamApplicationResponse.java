package com.neomango.team.dto;

import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamApplicationStatus;

public record TeamApplicationResponse(
	Long applicationId,
	Long teamId,
	String teamName,
	TeamApplicationStatus status,
	String message
) {

	public static TeamApplicationResponse from(TeamApplication teamApplication) {
		return new TeamApplicationResponse(
			teamApplication.getId(),
			teamApplication.getTeam().getId(),
			teamApplication.getTeam().getName(),
			teamApplication.getStatus(),
			teamApplication.getMessage()
		);
	}
}
