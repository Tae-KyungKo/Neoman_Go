package com.neomango.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neomango.team.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {
}

