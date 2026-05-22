package com.neomango.team.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.team.dto.TeamApplicationCreateRequest;
import com.neomango.team.dto.TeamApplicationOwnerResponse;
import com.neomango.team.dto.TeamApplicationResponse;
import com.neomango.team.dto.TeamApplicationSummaryResponse;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamApplicationStatus;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamMemberRole;
import com.neomango.team.entity.TeamMemberStatus;
import com.neomango.team.entity.TeamStatus;
import com.neomango.team.entity.UserCategoryMembership;
import com.neomango.team.exception.DuplicateTeamMemberException;
import com.neomango.team.exception.NotTeamOwnerException;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
import com.neomango.team.repository.UserCategoryMembershipRepository;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserStatus;
import com.neomango.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamApplicationService {

	private final TeamApplicationRepository teamApplicationRepository;
	private final TeamRepository teamRepository;
	private final TeamMemberRepository teamMemberRepository;
	private final UserCategoryMembershipRepository userCategoryMembershipRepository;
	private final UserRepository userRepository;

	public TeamApplicationResponse createApplication(
		Long teamId,
		Long applicantId,
		TeamApplicationCreateRequest request
	) {
		if (applicantId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		User applicant = userRepository.findById(applicantId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		validateActiveUser(applicant);

		Team team = findAccessibleTeam(teamId);
		validateApplicableTeam(team);
		validateApplicantIsNotTeamMember(team, applicant);
		validateApplicantIsNotSameCategoryMember(team, applicant);
		validateNoPendingApplication(team, applicant);

		TeamApplication teamApplication = TeamApplication.create(team, applicant, request.message());

		return TeamApplicationResponse.from(teamApplicationRepository.save(teamApplication));
	}

	public TeamApplicationResponse cancelApplication(Long applicationId, Long requesterId) {
		if (requesterId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		TeamApplication teamApplication = teamApplicationRepository.findByIdWithTeam(applicationId)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_APPLICATION_NOT_FOUND));
		validateApplicationOwner(teamApplication, requesterId);
		validatePendingApplication(teamApplication);

		teamApplication.cancel();

		return TeamApplicationResponse.from(teamApplication);
	}

	public TeamApplicationResponse approveApplication(Long applicationId, Long loginUserId) {
		if (loginUserId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		TeamApplication application = teamApplicationRepository.findByIdForUpdate(applicationId)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_APPLICATION_NOT_FOUND));
		application.validatePending();

		Team team = application.getTeam();
		User applicant = application.getApplicant();
		validateApplicationProcessOwner(team, loginUserId);
		validateNoDuplicateTeamMember(team, applicant);
		validateApplicantIsNotTeamMember(team, applicant);
		validateApplicantIsNotSameCategoryMember(team, applicant);
		createUserCategoryMembership(applicant, team);

		TeamMember teamMember = TeamMember.createMember(team, applicant);
		team.addMember(teamMember);
		teamMemberRepository.save(teamMember);

		application.approve();
		cancelOtherPendingApplicationsInSameCategory(application);

		return TeamApplicationResponse.from(application);
	}

	public TeamApplicationResponse rejectApplication(Long applicationId, Long loginUserId) {
		if (loginUserId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		TeamApplication application = teamApplicationRepository.findByIdForUpdate(applicationId)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_APPLICATION_NOT_FOUND));
		application.validatePending();

		validateApplicationProcessOwner(application.getTeam(), loginUserId);

		application.reject();

		return TeamApplicationResponse.from(application);
	}

	@Transactional(readOnly = true)
	public List<TeamApplicationSummaryResponse> getMyApplications(Long applicantId) {
		if (applicantId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return teamApplicationRepository.findByApplicantIdWithTeamOrderByCreatedAtDesc(applicantId)
			.stream()
			.map(TeamApplicationSummaryResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<TeamApplicationOwnerResponse> getPendingApplicationsForOwner(Long teamId, Long requesterId) {
		if (requesterId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		Team team = findAccessibleTeam(teamId);
		validateActiveOwner(team, requesterId);

		return teamApplicationRepository
			.findByTeamIdAndStatusWithApplicantOrderByCreatedAtAsc(team.getId(), TeamApplicationStatus.PENDING)
			.stream()
			.map(TeamApplicationOwnerResponse::from)
			.toList();
	}

	private void validateActiveUser(User user) {
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
	}

	private Team findAccessibleTeam(Long teamId) {
		return teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(teamId, TeamStatus.DELETED)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
	}

	private void validateApplicableTeam(Team team) {
		if (team.getStatus() == TeamStatus.DELETED || team.getDeletedAt() != null) {
			throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
		}

		if (team.getStatus() == TeamStatus.CLOSED) {
			throw new BusinessException(ErrorCode.TEAM_CLOSED);
		}

		if (team.getStatus() != TeamStatus.RECRUITING) {
			throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
		}
	}

	private void validateApplicantIsNotTeamMember(Team team, User applicant) {
		if (teamMemberRepository.existsByTeamIdAndUserIdAndStatus(
			team.getId(),
			applicant.getId(),
			TeamMemberStatus.ACTIVE
		)) {
			throw new BusinessException(ErrorCode.ALREADY_TEAM_MEMBER);
		}
	}

	private void validateApplicantIsNotSameCategoryMember(Team team, User applicant) {
		if (userCategoryMembershipRepository.existsByUserIdAndCategory(applicant.getId(), team.getCategory())) {
			throw new BusinessException(ErrorCode.ALREADY_CATEGORY_TEAM_MEMBER);
		}

		if (teamMemberRepository.existsActiveMemberByUserIdAndTeamCategory(
			applicant.getId(),
			team.getCategory()
		)) {
			throw new BusinessException(ErrorCode.ALREADY_CATEGORY_TEAM_MEMBER);
		}
	}

	private void createUserCategoryMembership(User applicant, Team team) {
		try {
			userCategoryMembershipRepository.saveAndFlush(
				UserCategoryMembership.create(applicant, team.getCategory(), team)
			);
		}
		catch (DataIntegrityViolationException exception) {
			// TODO: 여러 unique constraint를 한 곳에서 처리하게 되면 constraint name 기반 분기를 검토한다.
			throw new BusinessException(ErrorCode.ALREADY_CATEGORY_TEAM_MEMBER);
		}
	}

	private void validateNoDuplicateTeamMember(Team team, User applicant) {
		if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), applicant.getId())) {
			throw new DuplicateTeamMemberException();
		}
	}

	private void cancelOtherPendingApplicationsInSameCategory(TeamApplication approvedApplication) {
		teamApplicationRepository.findOtherPendingApplicationsInSameCategory(
				approvedApplication.getId(),
				approvedApplication.getApplicant().getId(),
				approvedApplication.getTeam().getCategory()
			)
			.forEach(TeamApplication::cancel);
	}

	private void validateNoPendingApplication(Team team, User applicant) {
		if (teamApplicationRepository.existsByTeamIdAndApplicantIdAndStatus(
			team.getId(),
			applicant.getId(),
			TeamApplicationStatus.PENDING
		)) {
			throw new BusinessException(ErrorCode.DUPLICATE_PENDING_TEAM_APPLICATION);
		}
	}

	private void validateApplicationOwner(TeamApplication teamApplication, Long requesterId) {
		if (!teamApplication.getApplicant().getId().equals(requesterId)) {
			throw new BusinessException(ErrorCode.TEAM_APPLICATION_CANCEL_FORBIDDEN);
		}
	}

	private void validatePendingApplication(TeamApplication teamApplication) {
		if (teamApplication.getStatus() != TeamApplicationStatus.PENDING) {
			throw new BusinessException(ErrorCode.ONLY_PENDING_TEAM_APPLICATION_CANCELABLE);
		}
	}

	private void validateActiveOwner(Team team, Long requesterId) {
		TeamMember ownerMember = teamMemberRepository.findByTeamIdAndRole(team.getId(), TeamMemberRole.OWNER)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_APPLICATION_LIST_OWNER_REQUIRED));

		if (!ownerMember.getUser().getId().equals(requesterId) || ownerMember.getStatus() != TeamMemberStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.TEAM_APPLICATION_LIST_OWNER_REQUIRED);
		}
	}

	private void validateApplicationProcessOwner(Team team, Long loginUserId) {
		TeamMember ownerMember = teamMemberRepository.findByTeamIdAndRole(team.getId(), TeamMemberRole.OWNER)
			.orElseThrow(NotTeamOwnerException::new);

		if (!ownerMember.getUser().getId().equals(loginUserId)
			|| ownerMember.getRole() != TeamMemberRole.OWNER
			|| ownerMember.getStatus() != TeamMemberStatus.ACTIVE) {
			throw new NotTeamOwnerException();
		}
	}
}
