package com.neomango.team.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.team.dto.TeamCreateRequest;
import com.neomango.team.dto.TeamDetailResponse;
import com.neomango.team.dto.TeamResponse;
import com.neomango.team.dto.TeamSummaryResponse;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamMemberRole;
import com.neomango.team.entity.TeamStatus;
import com.neomango.team.entity.UserCategoryMembership;
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
public class TeamService {

	private final TeamRepository teamRepository;
	private final TeamMemberRepository teamMemberRepository;
	private final UserCategoryMembershipRepository userCategoryMembershipRepository;
	private final UserRepository userRepository;

	public TeamResponse createTeam(Long userId, TeamCreateRequest request) {
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		User owner = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		validateActiveUser(owner);

		Team team = Team.create(
			request.name(),
			request.description(),
			request.category(),
			owner
		);
		validateOwnerHasNoCategoryMembership(owner.getId(), team.getCategory());

		Team savedTeam = teamRepository.save(team);
		createUserCategoryMembership(owner, savedTeam);

		return TeamResponse.from(savedTeam);
	}

	@Transactional(readOnly = true)
	public Page<TeamSummaryResponse> getTeams(String category, Pageable pageable) {
		String normalizedCategory = normalizeCategory(category);
		Page<Team> teams = StringUtils.hasText(normalizedCategory)
			? teamRepository.findByCategoryAndDeletedAtIsNull(normalizedCategory, pageable)
			: teamRepository.findByDeletedAtIsNull(pageable);

		Map<Long, TeamMember> ownerMembers = findOwnerMembersByTeamIds(teams.getContent());

		return teams.map(team -> TeamSummaryResponse.from(team, getOwnerMember(ownerMembers, team.getId())));
	}

	@Transactional(readOnly = true)
	public TeamDetailResponse getTeamDetail(Long teamId) {
		Team team = teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(teamId, TeamStatus.DELETED)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
		List<TeamMember> members = teamMemberRepository.findByTeamIdWithUser(teamId);
		TeamMember ownerMember = members.stream()
			.filter(member -> member.getRole() == TeamMemberRole.OWNER)
			.findFirst()
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_OWNER_NOT_FOUND));

		return TeamDetailResponse.from(team, ownerMember, members);
	}

	public void closeTeam(Long userId, Long teamId) {
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		Team team = teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(teamId, TeamStatus.DELETED)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
		TeamMember ownerMember = teamMemberRepository.findByTeamIdAndRole(teamId, TeamMemberRole.OWNER)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_OWNER_NOT_FOUND));

		validateActiveOwner(ownerMember, userId);
		team.close();
	}

	public void deleteTeam(Long userId, Long teamId) {
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		Team team = teamRepository.findByIdAndStatusNotAndDeletedAtIsNull(teamId, TeamStatus.DELETED)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
		TeamMember ownerMember = teamMemberRepository.findByTeamIdAndRole(teamId, TeamMemberRole.OWNER)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_OWNER_NOT_FOUND));

		validateActiveOwner(ownerMember, userId);
		team.softDelete();
	}

	private Map<Long, TeamMember> findOwnerMembersByTeamIds(List<Team> teams) {
		if (teams.isEmpty()) {
			return Collections.emptyMap();
		}

		List<Long> teamIds = teams.stream()
			.map(Team::getId)
			.toList();

		return teamMemberRepository.findOwnersByTeamIds(teamIds)
			.stream()
			.collect(Collectors.toMap(teamMember -> teamMember.getTeam().getId(), Function.identity()));
	}

	private TeamMember getOwnerMember(Map<Long, TeamMember> ownerMembers, Long teamId) {
		TeamMember ownerMember = ownerMembers.get(teamId);
		if (ownerMember == null) {
			throw new BusinessException(ErrorCode.TEAM_OWNER_NOT_FOUND);
		}
		return ownerMember;
	}

	private String normalizeCategory(String category) {
		return StringUtils.hasText(category) ? category.trim() : category;
	}

	private void validateActiveUser(User user) {
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
	}

	private void validateOwnerHasNoCategoryMembership(Long userId, String category) {
		if (userCategoryMembershipRepository.existsByUserIdAndCategory(userId, category)) {
			throw new BusinessException(ErrorCode.ALREADY_CATEGORY_TEAM_MEMBER);
		}
	}

	private void createUserCategoryMembership(User owner, Team team) {
		try {
			userCategoryMembershipRepository.saveAndFlush(
				UserCategoryMembership.create(owner, team.getCategory(), team)
			);
		}
		catch (DataIntegrityViolationException exception) {
			throw new BusinessException(ErrorCode.ALREADY_CATEGORY_TEAM_MEMBER);
		}
	}

	private void validateActiveOwner(TeamMember ownerMember, Long userId) {
		if (!ownerMember.getUser().getId().equals(userId) || !ownerMember.isActive()) {
			throw new BusinessException(ErrorCode.TEAM_OWNER_REQUIRED);
		}
	}
}
