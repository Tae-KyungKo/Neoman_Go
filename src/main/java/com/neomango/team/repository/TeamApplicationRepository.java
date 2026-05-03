package com.neomango.team.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamApplicationStatus;

import jakarta.persistence.LockModeType;

public interface TeamApplicationRepository extends JpaRepository<TeamApplication, Long> {

	boolean existsByTeamIdAndUserIdAndStatus(
		Long teamId,
		Long userId,
		TeamApplicationStatus status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select application
		from TeamApplication application
		join fetch application.team
		join fetch application.user
		where application.id = :applicationId
		""")
	Optional<TeamApplication> findByIdWithLock(@Param("applicationId") Long applicationId);
}
