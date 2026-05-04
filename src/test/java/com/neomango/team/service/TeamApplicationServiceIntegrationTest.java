package com.neomango.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.team.dto.TeamApplicationResponse;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamApplicationStatus;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
import com.neomango.user.entity.User;
import com.neomango.user.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest
class TeamApplicationServiceIntegrationTest {

	@Autowired
	private TeamApplicationService teamApplicationService;

	@Autowired
	private TeamApplicationRepository teamApplicationRepository;

	@Autowired
	private TeamMemberRepository teamMemberRepository;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		teamApplicationRepository.deleteAll();
		teamMemberRepository.deleteAll();
		teamRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("가입 신청에 성공하면 PENDING 상태의 TeamApplication이 생성된다")
	void apply_success() {
		TeamFixture fixture = createTeamFixture();
		User applicant = saveUser("applicant@test.com", "applicant");

		TeamApplicationResponse application = teamApplicationService.apply(
			fixture.team().getId(),
			applicant.getId(),
			"가입하고 싶습니다."
		);

		assertThat(application.applicationId()).isNotNull();
		assertThat(application.teamId()).isEqualTo(fixture.team().getId());
		assertThat(application.userId()).isEqualTo(applicant.getId());
		assertThat(application.status()).isEqualTo(TeamApplicationStatus.PENDING);
		assertThat(application.message()).isEqualTo("가입하고 싶습니다.");
		assertThat(application.createdAt()).isNotNull();
		assertThat(application.processedAt()).isNull();
	}

	@Test
	@DisplayName("이미 팀 멤버인 사용자는 가입 신청할 수 없다")
	void apply_fail_whenAlreadyTeamMember() {
		TeamFixture fixture = createTeamFixture();

		assertThatThrownBy(() -> teamApplicationService.apply(
			fixture.team().getId(),
			fixture.owner().getId(),
			"이미 OWNER입니다."
		))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.ALREADY_TEAM_MEMBER);
	}

	@Test
	@DisplayName("PENDING 신청이 이미 있으면 중복 신청은 실패한다 - 동시 요청 race condition은 generated column 도입 전까지 남아 있다")
	void apply_fail_whenPendingApplicationAlreadyExists() {
		TeamFixture fixture = createTeamFixture();
		User applicant = saveUser("applicant@test.com", "applicant");
		teamApplicationService.apply(fixture.team().getId(), applicant.getId(), "첫 신청");

		assertThatThrownBy(() -> teamApplicationService.apply(
			fixture.team().getId(),
			applicant.getId(),
			"중복 신청"
		))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DUPLICATE_PENDING_TEAM_APPLICATION);
	}

	@Test
	@DisplayName("REJECTED 이후에는 같은 팀에 새 row로 재신청할 수 있다")
	void apply_success_afterRejected() {
		TeamFixture fixture = createTeamFixture();
		User applicant = saveUser("applicant@test.com", "applicant");
		TeamApplicationResponse rejectedApplication = teamApplicationService.apply(
			fixture.team().getId(),
			applicant.getId(),
			"첫 신청"
		);
		teamApplicationService.reject(rejectedApplication.applicationId(), fixture.owner().getId());

		TeamApplicationResponse reappliedApplication = teamApplicationService.apply(
			fixture.team().getId(),
			applicant.getId(),
			"재신청"
		);

		assertThat(reappliedApplication.applicationId()).isNotEqualTo(rejectedApplication.applicationId());
		assertThat(reappliedApplication.status()).isEqualTo(TeamApplicationStatus.PENDING);
		assertThat(teamApplicationRepository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("APPROVED 이후에는 이미 팀 멤버이므로 다시 신청할 수 없다")
	void apply_fail_afterApproved() {
		TeamFixture fixture = createTeamFixture();
		User applicant = saveUser("applicant@test.com", "applicant");
		TeamApplicationResponse application = teamApplicationService.apply(
			fixture.team().getId(),
			applicant.getId(),
			"첫 신청"
		);
		teamApplicationService.approve(application.applicationId(), fixture.owner().getId());

		assertThatThrownBy(() -> teamApplicationService.apply(
			fixture.team().getId(),
			applicant.getId(),
			"승인 후 재신청"
		))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.ALREADY_TEAM_MEMBER);
	}

	@Test
	@DisplayName("OWNER는 PENDING 가입 신청을 승인할 수 있다")
	void approve_success_byOwner() {
		TeamFixture fixture = createTeamFixture();
		User applicant = saveUser("applicant@test.com", "applicant");
		TeamApplicationResponse application = teamApplicationService.apply(
			fixture.team().getId(),
			applicant.getId(),
			"가입 신청"
		);

		TeamApplicationResponse approvedApplication = teamApplicationService.approve(
			application.applicationId(),
			fixture.owner().getId()
		);

		assertThat(approvedApplication.status()).isEqualTo(TeamApplicationStatus.APPROVED);
		assertThat(approvedApplication.processedAt()).isNotNull();
	}

	@Test
	@DisplayName("OWNER가 아닌 사용자는 가입 신청을 승인할 수 없다")
	void approve_fail_whenApproverIsNotOwner() {
		TeamFixture fixture = createTeamFixture();
		User applicant = saveUser("applicant@test.com", "applicant");
		User notOwner = saveUser("not-owner@test.com", "notOwner");
		TeamApplicationResponse application = teamApplicationService.apply(
			fixture.team().getId(),
			applicant.getId(),
			"가입 신청"
		);

		assertThatThrownBy(() -> teamApplicationService.approve(application.applicationId(), notOwner.getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.TEAM_OWNER_REQUIRED);
	}

	@Test
	@DisplayName("REJECTED 신청은 승인할 수 없다")
	void approve_fail_whenApplicationRejected() {
		TeamFixture fixture = createTeamFixture();
		User applicant = saveUser("applicant@test.com", "applicant");
		TeamApplicationResponse application = teamApplicationService.apply(
			fixture.team().getId(),
			applicant.getId(),
			"가입 신청"
		);
		teamApplicationService.reject(application.applicationId(), fixture.owner().getId());

		assertThatThrownBy(() -> teamApplicationService.approve(application.applicationId(), fixture.owner().getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_TEAM_APPLICATION_STATUS);
	}

	@Test
	@DisplayName("이미 처리된 신청은 거절할 수 없다")
	void reject_fail_whenApplicationAlreadyProcessed() {
		TeamFixture fixture = createTeamFixture();
		User applicant = saveUser("applicant@test.com", "applicant");
		TeamApplicationResponse application = teamApplicationService.apply(
			fixture.team().getId(),
			applicant.getId(),
			"가입 신청"
		);
		teamApplicationService.approve(application.applicationId(), fixture.owner().getId());

		assertThatThrownBy(() -> teamApplicationService.reject(application.applicationId(), fixture.owner().getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_TEAM_APPLICATION_STATUS);
	}

	@Test
	@DisplayName("승인 시 TeamMember가 생성된다")
	void approve_createsTeamMember() {
		TeamFixture fixture = createTeamFixture();
		User applicant = saveUser("applicant@test.com", "applicant");
		TeamApplicationResponse application = teamApplicationService.apply(
			fixture.team().getId(),
			applicant.getId(),
			"가입 신청"
		);

		teamApplicationService.approve(application.applicationId(), fixture.owner().getId());

		assertThat(teamMemberRepository.existsByTeamIdAndUserId(
			fixture.team().getId(),
			applicant.getId()
		)).isTrue();
	}

	private TeamFixture createTeamFixture() {
		User owner = saveUser("owner@test.com", "owner");
		Team team = teamRepository.save(Team.create("테스트 팀", owner));
		return new TeamFixture(team, owner);
	}

	private User saveUser(String email, String nickname) {
		return userRepository.save(User.create(email, "encoded-password", nickname));
	}

	private record TeamFixture(
		Team team,
		User owner
	) {
	}
}
