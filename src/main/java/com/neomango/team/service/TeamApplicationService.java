package com.neomango.team.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.team.dto.TeamApplicationResponse;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamApplicationStatus;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamRole;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
import com.neomango.user.entity.User;
import com.neomango.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamApplicationService {

	private final TeamApplicationRepository teamApplicationRepository;
	private final TeamMemberRepository teamMemberRepository;
	private final TeamRepository teamRepository;
	private final UserService userService;

	public TeamApplicationResponse apply(Long teamId, Long userId, String message) {
		Team team = getTeam(teamId);
		User user = userService.getById(userId);

		validateNotTeamMember(teamId, userId);
		validateNoPendingApplication(teamId, userId);

		TeamApplication application = teamApplicationRepository.save(TeamApplication.create(team, user, message));
		return TeamApplicationResponse.from(application);
	}

	public TeamApplicationResponse approve(Long applicationId, Long approverId) {
		TeamApplication application = getApplicationWithLock(applicationId);
		Team team = application.getTeam();
		User applicant = application.getUser();

		validateOwner(team.getId(), approverId);
		validateNotTeamMember(team.getId(), applicant.getId());

		try {
			teamMemberRepository.saveAndFlush(TeamMember.createMember(team, applicant));
		} catch (DataIntegrityViolationException exception) {
			throw new BusinessException(ErrorCode.DUPLICATE_TEAM_MEMBER);
		}

		application.approve();
		return TeamApplicationResponse.from(application);
	}

	public TeamApplicationResponse reject(Long applicationId, Long approverId) {
		TeamApplication application = getApplicationWithLock(applicationId);

		validateOwner(application.getTeam().getId(), approverId);
		application.reject();

		return TeamApplicationResponse.from(application);
	}

	private Team getTeam(Long teamId) {
		return teamRepository.findById(teamId)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
	}

	private TeamApplication getApplicationWithLock(Long applicationId) {
		return teamApplicationRepository.findByIdWithLock(applicationId)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_APPLICATION_NOT_FOUND));
	}

	private void validateNotTeamMember(Long teamId, Long userId) {
		if (teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
			throw new BusinessException(ErrorCode.ALREADY_TEAM_MEMBER);
		}
	}

	private void validateNoPendingApplication(Long teamId, Long userId) {
		boolean existsPendingApplication = teamApplicationRepository.existsByTeamIdAndUserIdAndStatus(
			teamId,
			userId,
			TeamApplicationStatus.PENDING
		);

		if (existsPendingApplication) {
			throw new BusinessException(ErrorCode.DUPLICATE_PENDING_TEAM_APPLICATION);
		}
	}

	private void validateOwner(Long teamId, Long userId) {
		if (!teamMemberRepository.existsByTeamIdAndUserIdAndRole(teamId, userId, TeamRole.OWNER)) {
			throw new BusinessException(ErrorCode.TEAM_OWNER_REQUIRED);
		}
	}
}
