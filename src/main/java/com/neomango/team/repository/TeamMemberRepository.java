package com.neomango.team.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamMemberRole;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

	@Query("""
		select teamMember
		from TeamMember teamMember
		join fetch teamMember.team
		join fetch teamMember.user
		where teamMember.team.id in :teamIds
			and teamMember.role = com.neomango.team.entity.TeamMemberRole.OWNER
		""")
	List<TeamMember> findOwnersByTeamIds(@Param("teamIds") Collection<Long> teamIds);

	@Query("""
		select teamMember
		from TeamMember teamMember
		join fetch teamMember.user
		where teamMember.team.id = :teamId
		order by teamMember.joinedAt asc, teamMember.id asc
		""")
	List<TeamMember> findByTeamIdWithUser(@Param("teamId") Long teamId);

	Optional<TeamMember> findByTeamIdAndRole(Long teamId, TeamMemberRole role);
}

