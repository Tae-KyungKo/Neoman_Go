package com.neomango.team.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamStatus;

public interface TeamRepository extends JpaRepository<Team, Long> {

	Page<Team> findByDeletedAtIsNull(Pageable pageable);

	Page<Team> findByCategoryAndDeletedAtIsNull(String category, Pageable pageable);

	Optional<Team> findByIdAndDeletedAtIsNull(Long id);

	Optional<Team> findByIdAndStatusNotAndDeletedAtIsNull(Long id, TeamStatus status);
}

