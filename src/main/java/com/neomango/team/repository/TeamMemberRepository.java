package com.neomango.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neomango.team.entity.TeamMember;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
}

