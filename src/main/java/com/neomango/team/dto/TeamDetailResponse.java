package com.neomango.team.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamStatus;

public record TeamDetailResponse(
	Long id,
	String name,
	String description,
	String category,
	Integer currentMemberCount,
	Integer maxMemberCount,
	TeamStatus status,
	TeamMemberResponse owner,
	List<TeamMemberResponse> members,
	LocalDateTime createdAt
) {

	public static TeamDetailResponse from(Team team, TeamMember ownerMember, List<TeamMember> members) {
		return new TeamDetailResponse(
			team.getId(),
			team.getName(),
			team.getDescription(),
			team.getCategory(),
			team.getCurrentMemberCount(),
			team.getMaxMemberCount(),
			team.getStatus(),
			TeamMemberResponse.from(ownerMember),
			members.stream()
				.map(TeamMemberResponse::from)
				.toList(),
			team.getCreatedAt()
		);
	}
}
