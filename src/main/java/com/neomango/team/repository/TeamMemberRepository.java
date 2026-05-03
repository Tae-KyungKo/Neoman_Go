package com.neomango.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamRole;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

	boolean existsByTeamIdAndUserId(Long teamId, Long userId);

	boolean existsByTeamIdAndUserIdAndRole(Long teamId, Long userId, TeamRole role);
}

