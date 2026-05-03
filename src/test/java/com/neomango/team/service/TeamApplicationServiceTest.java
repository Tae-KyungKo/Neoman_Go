package com.neomango.team.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamRole;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
import com.neomango.user.entity.User;
import com.neomango.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class TeamApplicationServiceTest {

	@Mock
	private TeamApplicationRepository teamApplicationRepository;

	@Mock
	private TeamMemberRepository teamMemberRepository;

	@Mock
	private TeamRepository teamRepository;

	@Mock
	private UserService userService;

	@InjectMocks
	private TeamApplicationService teamApplicationService;

	@Test
	@DisplayName("승인 중 TeamMember unique constraint 위반은 DUPLICATE_TEAM_MEMBER로 변환된다")
	void approve_convertsTeamMemberUniqueConstraintViolation() {
		User owner = User.create("owner@test.com", "password", "owner");
		User applicant = User.create("applicant@test.com", "password", "applicant");
		Team team = Team.create("테스트 팀", owner);
		TeamApplication application = TeamApplication.create(team, applicant, "가입 신청");

		when(teamApplicationRepository.findByIdWithLock(1L)).thenReturn(Optional.of(application));
		when(teamMemberRepository.existsByTeamIdAndUserIdAndRole(
			team.getId(),
			99L,
			TeamRole.OWNER
		)).thenReturn(true);
		when(teamMemberRepository.existsByTeamIdAndUserId(team.getId(), applicant.getId())).thenReturn(false);
		when(teamMemberRepository.saveAndFlush(any(TeamMember.class)))
			.thenThrow(new DataIntegrityViolationException("duplicate team member"));

		assertThatThrownBy(() -> teamApplicationService.approve(1L, 99L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DUPLICATE_TEAM_MEMBER);
	}
}
