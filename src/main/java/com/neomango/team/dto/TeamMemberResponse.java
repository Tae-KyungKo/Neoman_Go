package com.neomango.team.dto;

import java.time.LocalDateTime;

import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamMemberRole;
import com.neomango.team.entity.TeamMemberStatus;
import com.neomango.user.entity.User;

public record TeamMemberResponse(
	Long userId,
	String nickname,
	TeamMemberRole role,
	TeamMemberStatus status,
	LocalDateTime joinedAt
) {

	public static TeamMemberResponse from(TeamMember teamMember) {
		User user = teamMember.getUser();
		return new TeamMemberResponse(
			user.getId(),
			user.getNickname(),
			teamMember.getRole(),
			teamMember.getStatus(),
			teamMember.getJoinedAt()
		);
	}
}
