package com.neomango.team.dto;

import java.time.LocalDateTime;

import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamApplicationStatus;

public record TeamApplicationResponse(
	Long applicationId,
	Long teamId,
	Long userId,
	TeamApplicationStatus status,
	String message,
	LocalDateTime createdAt,
	LocalDateTime processedAt
) {

	public static TeamApplicationResponse from(TeamApplication application) {
		return new TeamApplicationResponse(
			application.getId(),
			application.getTeam().getId(),
			application.getUser().getId(),
			application.getStatus(),
			application.getMessage(),
			application.getCreatedAt(),
			application.getProcessedAt()
		);
	}
}
