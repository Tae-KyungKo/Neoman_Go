package com.neomango.team.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.notification.service.NotificationService;
import com.neomango.team.dto.TeamMemberListResponse;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamApplicationStatus;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamMemberRole;
import com.neomango.team.entity.TeamStatus;
import com.neomango.team.exception.CannotKickOwnerException;
import com.neomango.team.exception.CannotKickSelfException;
import com.neomango.team.exception.CannotLeaveOwnerWithoutDelegationException;
import com.neomango.team.exception.InvalidOwnerDelegationTargetException;
import com.neomango.team.exception.NotTeamOwnerException;
import com.neomango.team.exception.NotTeamMemberException;
import com.neomango.team.exception.TeamOwnerInvariantViolationException;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
import com.neomango.team.repository.UserCategoryMembershipRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamMemberService {

	private final TeamRepository teamRepository;
	private final TeamApplicationRepository teamApplicationRepository;
	private final TeamMemberRepository teamMemberRepository;
	private final UserCategoryMembershipRepository userCategoryMembershipRepository;
	private final NotificationService notificationService;

	public List<TeamMemberListResponse> getActiveTeamMembers(Long teamId) {
		teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(teamId, TeamStatus.DELETED)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

		return teamMemberRepository.findActiveMembersByTeamId(teamId)
			.stream()
			.map(TeamMemberListResponse::from)
			.toList();
	}

	@Transactional
	public void leaveTeam(Long teamId, Long userId) {
		Team team = teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(teamId, TeamStatus.DELETED)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
		TeamMember teamMember = teamMemberRepository.findActiveMemberByTeamIdAndUserId(teamId, userId)
			.orElseThrow(NotTeamMemberException::new);

		if (teamMember.isOwner()) {
			leaveOwnerTeam(team, teamMember);
			return;
		}

		deactivateMemberAndReleaseCategory(team, teamMember);
		createTeamMemberLeftNotifications(team, teamMember);
	}

	@Transactional
	public void kickMember(Long teamId, Long targetTeamMemberId, Long requesterUserId) {
		Team team = teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(teamId, TeamStatus.DELETED)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
		TeamMember requester = teamMemberRepository.findActiveMemberByTeamIdAndUserId(teamId, requesterUserId)
			.orElseThrow(NotTeamOwnerException::new);

		if (!requester.isOwner()) {
			throw new NotTeamOwnerException();
		}

		TeamMember target = teamMemberRepository.findActiveMemberById(targetTeamMemberId)
			.orElseThrow(NotTeamMemberException::new);
		if (!target.getTeam().getId().equals(teamId)) {
			throw new NotTeamMemberException();
		}
		if (requester.getId().equals(target.getId())) {
			throw new CannotKickSelfException();
		}
		if (target.isOwner()) {
			throw new CannotKickOwnerException();
		}

		target.deactivate();
		userCategoryMembershipRepository.deleteByUserIdAndCategory(target.getUser().getId(), team.getCategory());
	}

	@Transactional
	public void delegateOwner(Long teamId, Long requesterUserId, Long targetTeamMemberId) {
		teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(teamId, TeamStatus.DELETED)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
		TeamMember requester = teamMemberRepository.findActiveMemberByTeamIdAndUserId(teamId, requesterUserId)
			.orElseThrow(NotTeamOwnerException::new);

		if (!requester.isOwner()) {
			throw new NotTeamOwnerException();
		}

		TeamMember target = teamMemberRepository.findActiveMemberById(targetTeamMemberId)
			.orElseThrow(NotTeamMemberException::new);
		if (!target.getTeam().getId().equals(teamId)) {
			throw new NotTeamMemberException();
		}
		if (requester.getId().equals(target.getId()) || target.isOwner()) {
			throw new InvalidOwnerDelegationTargetException();
		}

		requester.changeRole(TeamMemberRole.MEMBER);
		target.changeRole(TeamMemberRole.OWNER);
		if (teamMemberRepository.countActiveOwnersByTeamId(teamId) != 1) {
			throw new TeamOwnerInvariantViolationException();
		}
	}

	private void leaveOwnerTeam(Team team, TeamMember ownerMember) {
		long activeMemberCount = teamMemberRepository.countActiveMembersByTeamId(team.getId());
		if (activeMemberCount > 1) {
			throw new CannotLeaveOwnerWithoutDelegationException();
		}

		team.softDelete();
		deactivateMemberAndReleaseCategory(team, ownerMember);
		teamApplicationRepository.findByTeamIdAndStatusOrderByCreatedAtAsc(
				team.getId(),
				TeamApplicationStatus.PENDING
			)
			.forEach(TeamApplication::reject);
	}

	private void deactivateMemberAndReleaseCategory(Team team, TeamMember teamMember) {
		teamMember.deactivate();
		userCategoryMembershipRepository.deleteByUserIdAndCategory(
			teamMember.getUser().getId(),
			team.getCategory()
		);
	}

	private void createTeamMemberLeftNotifications(Team team, TeamMember leftMember) {
		Set<Long> receiverIds = new LinkedHashSet<>();
		teamMemberRepository.findActiveMembersByTeamId(team.getId())
			.stream()
			.map(teamMember -> teamMember.getUser().getId())
			.filter(receiverId -> !receiverId.equals(leftMember.getUser().getId()))
			.forEach(receiverIds::add);

		receiverIds.forEach(receiverId -> notificationService.createTeamMemberLeftNotification(
			receiverId,
			leftMember.getUser().getId(),
			team.getName(),
			leftMember.getUser().getNickname(),
			team.getId()
		));
	}
}
