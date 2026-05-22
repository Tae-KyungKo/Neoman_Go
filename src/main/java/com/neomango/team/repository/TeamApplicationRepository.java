package com.neomango.team.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamApplicationStatus;

import jakarta.persistence.LockModeType;

public interface TeamApplicationRepository extends JpaRepository<TeamApplication, Long> {

	boolean existsByTeamIdAndApplicantIdAndStatus(
		Long teamId,
		Long applicantId,
		TeamApplicationStatus status
	);

	List<TeamApplication> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);

	List<TeamApplication> findByTeamIdAndStatusOrderByCreatedAtAsc(
		Long teamId,
		TeamApplicationStatus status
	);

	@Query("""
		select application
		from TeamApplication application
		join fetch application.team
		where application.id = :applicationId
		""")
	Optional<TeamApplication> findByIdWithTeam(@Param("applicationId") Long applicationId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select application
		from TeamApplication application
		join fetch application.team
		join fetch application.applicant
		where application.id = :applicationId
		""")
	Optional<TeamApplication> findByIdForUpdate(@Param("applicationId") Long applicationId);

	@Query("""
		select application
		from TeamApplication application
		join fetch application.team
		where application.applicant.id = :applicantId
		order by application.createdAt desc
		""")
	List<TeamApplication> findByApplicantIdWithTeamOrderByCreatedAtDesc(@Param("applicantId") Long applicantId);

	@Query("""
		select application
		from TeamApplication application
		join fetch application.team
		join fetch application.applicant
		where application.team.id = :teamId
			and application.status = :status
		order by application.createdAt asc
		""")
	List<TeamApplication> findByTeamIdAndStatusWithApplicantOrderByCreatedAtAsc(
		@Param("teamId") Long teamId,
		@Param("status") TeamApplicationStatus status
	);

	@Query("""
		select application
		from TeamApplication application
		join fetch application.team team
		where application.applicant.id = :applicantId
			and team.category = :category
			and application.status = com.neomango.team.entity.TeamApplicationStatus.PENDING
			and application.id <> :applicationId
		""")
	List<TeamApplication> findOtherPendingApplicationsInSameCategory(
		@Param("applicationId") Long applicationId,
		@Param("applicantId") Long applicantId,
		@Param("category") String category
	);
}
