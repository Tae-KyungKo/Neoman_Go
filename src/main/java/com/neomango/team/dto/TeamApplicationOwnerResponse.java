package com.neomango.team.dto;

import java.time.LocalDateTime;

import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamApplicationStatus;
import com.neomango.user.entity.User;

public record TeamApplicationOwnerResponse(
	Long applicationId,
	Long applicantId,
	String applicantNickname,
	Long teamId,
	String teamName,
	TeamApplicationStatus status,
	String message,
	LocalDateTime createdAt
) {

	public static TeamApplicationOwnerResponse from(TeamApplication teamApplication) {
		Team team = teamApplication.getTeam();
		User applicant = teamApplication.getApplicant();
		return new TeamApplicationOwnerResponse(
			teamApplication.getId(),
			applicant.getId(),
			applicant.getNickname(),
			team.getId(),
			team.getName(),
			teamApplication.getStatus(),
			teamApplication.getMessage(),
			teamApplication.getCreatedAt()
		);
	}
}
