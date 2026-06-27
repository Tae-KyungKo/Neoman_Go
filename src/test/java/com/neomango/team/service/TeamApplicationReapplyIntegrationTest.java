package com.neomango.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.notification.repository.NotificationRepository;
import com.neomango.team.dto.TeamApplicationCreateRequest;
import com.neomango.team.dto.TeamApplicationResponse;
import com.neomango.team.dto.TeamMemberListResponse;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamApplicationStatus;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamMemberRole;
import com.neomango.team.entity.TeamMemberStatus;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
import com.neomango.team.repository.UserCategoryMembershipRepository;
import com.neomango.user.entity.User;
import com.neomango.user.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest
class TeamApplicationReapplyIntegrationTest {

	private static final String CATEGORY = "LEAGUE_OF_LEGENDS";

	@Autowired
	private TeamApplicationService teamApplicationService;

	@Autowired
	private TeamMemberService teamMemberService;

	@Autowired
	private TeamApplicationRepository teamApplicationRepository;

	@Autowired
	private TeamMemberRepository teamMemberRepository;

	@Autowired
	private UserCategoryMembershipRepository userCategoryMembershipRepository;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@BeforeEach
	void setUp() {
		cleanUp();
	}

	@AfterEach
	void tearDown() {
		cleanUp();
	}

	private void cleanUp() {
		notificationRepository.deleteAll();
		teamApplicationRepository.deleteAll();
		userCategoryMembershipRepository.deleteAll();
		teamMemberRepository.deleteAll();
		teamRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void leftMemberCanReapplyAndBeApprovedWithoutDuplicateTeamMemberRow() {
		ReapplyFixture fixture = createLeftOwnerFixture();

		TeamApplicationResponse reapplyResponse = teamApplicationService.createApplication(
			fixture.team().getId(),
			fixture.tester1().getId(),
			request("rejoin")
		);

		TeamApplicationResponse approvedResponse = teamApplicationService.approveApplication(
			reapplyResponse.applicationId(),
			fixture.tester2().getId()
		);

		TeamMember rejoinedMember = teamMemberRepository
			.findByTeamIdAndUserId(fixture.team().getId(), fixture.tester1().getId())
			.orElseThrow();
		TeamApplication approvedApplication = teamApplicationRepository
			.findById(reapplyResponse.applicationId())
			.orElseThrow();

		assertThat(approvedResponse.status()).isEqualTo(TeamApplicationStatus.APPROVED);
		assertThat(approvedApplication.getStatus()).isEqualTo(TeamApplicationStatus.APPROVED);
		assertThat(rejoinedMember.getStatus()).isEqualTo(TeamMemberStatus.ACTIVE);
		assertThat(rejoinedMember.getRole()).isEqualTo(TeamMemberRole.MEMBER);
		assertThat(teamMemberRepository.countByTeamIdAndUserId(fixture.team().getId(), fixture.tester1().getId()))
			.isEqualTo(1);
		assertThat(teamMemberService.getActiveTeamMembers(fixture.team().getId()))
			.extracting(TeamMemberListResponse::userId)
			.contains(fixture.tester1().getId());
	}

	@Test
	void activeMemberCannotBeApprovedAgainAndDuplicateTeamMemberRowIsNotCreated() {
		User owner = userRepository.save(User.create("owner-active@test.com", "encoded-password", "owner"));
		User activeMember = userRepository.save(User.create("member-active@test.com", "encoded-password", "member"));
		Team team = teamRepository.save(Team.create("t1", null, CATEGORY, owner));
		TeamMember member = TeamMember.createMember(team, activeMember);
		team.addMember(member);
		teamMemberRepository.save(member);
		TeamApplication application = teamApplicationRepository.save(
			TeamApplication.create(team, activeMember, "duplicate")
		);

		assertThatThrownBy(() -> teamApplicationService.approveApplication(application.getId(), owner.getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.ALREADY_TEAM_MEMBER);
		assertThat(teamMemberRepository.countByTeamIdAndUserId(team.getId(), activeMember.getId())).isEqualTo(1);
	}

	@Test
	void approvingReapplyReactivatesExistingInactiveTeamMemberRow() {
		ReapplyFixture fixture = createLeftOwnerFixture();
		TeamMember inactiveMember = teamMemberRepository
			.findByTeamIdAndUserId(fixture.team().getId(), fixture.tester1().getId())
			.orElseThrow();
		Long inactiveMemberId = inactiveMember.getId();
		TeamApplicationResponse reapplyResponse = teamApplicationService.createApplication(
			fixture.team().getId(),
			fixture.tester1().getId(),
			request("rejoin")
		);

		teamApplicationService.approveApplication(reapplyResponse.applicationId(), fixture.tester2().getId());

		TeamMember reactivatedMember = teamMemberRepository
			.findByTeamIdAndUserId(fixture.team().getId(), fixture.tester1().getId())
			.orElseThrow();
		assertThat(reactivatedMember.getId()).isEqualTo(inactiveMemberId);
		assertThat(reactivatedMember.getStatus()).isEqualTo(TeamMemberStatus.ACTIVE);
		assertThat(teamMemberRepository.countByTeamIdAndUserId(fixture.team().getId(), fixture.tester1().getId()))
			.isEqualTo(1);
	}

	@Test
	void leftMemberCanCreateReapplyButCannotCreateDuplicatePendingApplication() {
		ReapplyFixture fixture = createLeftOwnerFixture();

		TeamApplicationResponse response = teamApplicationService.createApplication(
			fixture.team().getId(),
			fixture.tester1().getId(),
			request("rejoin")
		);

		assertThat(response.status()).isEqualTo(TeamApplicationStatus.PENDING);
		assertThatThrownBy(() -> teamApplicationService.createApplication(
			fixture.team().getId(),
			fixture.tester1().getId(),
			request("duplicate")
		))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DUPLICATE_PENDING_TEAM_APPLICATION);
	}

	@Test
	void rejectingLeftMemberReapplyKeepsTeamMemberInactive() {
		ReapplyFixture fixture = createLeftOwnerFixture();
		TeamApplicationResponse reapplyResponse = teamApplicationService.createApplication(
			fixture.team().getId(),
			fixture.tester1().getId(),
			request("rejoin")
		);

		TeamApplicationResponse rejectedResponse = teamApplicationService.rejectApplication(
			reapplyResponse.applicationId(),
			fixture.tester2().getId()
		);

		TeamApplication rejectedApplication = teamApplicationRepository
			.findById(reapplyResponse.applicationId())
			.orElseThrow();
		TeamMember leftMember = teamMemberRepository
			.findByTeamIdAndUserId(fixture.team().getId(), fixture.tester1().getId())
			.orElseThrow();

		assertThat(rejectedResponse.status()).isEqualTo(TeamApplicationStatus.REJECTED);
		assertThat(rejectedApplication.getStatus()).isEqualTo(TeamApplicationStatus.REJECTED);
		assertThat(leftMember.getStatus()).isEqualTo(TeamMemberStatus.INACTIVE);
		assertThat(teamMemberRepository.countByTeamIdAndUserId(fixture.team().getId(), fixture.tester1().getId()))
			.isEqualTo(1);
	}

	private ReapplyFixture createLeftOwnerFixture() {
		User tester1 = userRepository.save(User.create("tester1@test.com", "encoded-password", "tester1"));
		User tester2 = userRepository.save(User.create("tester2@test.com", "encoded-password", "tester2"));
		Team team = teamRepository.save(Team.create("t1", null, CATEGORY, tester1));

		TeamApplicationResponse tester2Application = teamApplicationService.createApplication(
			team.getId(),
			tester2.getId(),
			request("join")
		);
		teamApplicationService.approveApplication(tester2Application.applicationId(), tester1.getId());
		TeamMember tester2Member = teamMemberRepository.findActiveMemberByTeamIdAndUserId(
			team.getId(),
			tester2.getId()
		).orElseThrow();
		teamMemberService.delegateOwner(team.getId(), tester1.getId(), tester2Member.getId());
		teamMemberService.leaveTeam(team.getId(), tester1.getId());

		return new ReapplyFixture(tester1, tester2, team);
	}

	private TeamApplicationCreateRequest request(String message) {
		return new TeamApplicationCreateRequest(message);
	}

	private record ReapplyFixture(User tester1, User tester2, Team team) {
	}
}
