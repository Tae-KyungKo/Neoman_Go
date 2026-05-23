package com.neomango.team.dto;

import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamMemberRole;
import com.neomango.team.entity.TeamMemberStatus;
import com.neomango.user.entity.User;

public record TeamMemberListResponse(
	Long teamMemberId,
	Long userId,
	String nickname,
	TeamMemberRole role,
	TeamMemberStatus status
) {

	public static TeamMemberListResponse from(TeamMember teamMember) {
		User user = teamMember.getUser();
		return new TeamMemberListResponse(
			teamMember.getId(),
			user.getId(),
			user.getNickname(),
			teamMember.getRole(),
			teamMember.getStatus()
		);
	}
}
