package com.neomango.team.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.neomango.team.dto.TeamApplicationResponse;
import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamApplication;
import com.neomango.team.entity.TeamApplicationStatus;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
import com.neomango.team.repository.UserCategoryMembershipRepository;
import com.neomango.user.entity.User;
import com.neomango.user.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest
class TeamApplicationConcurrencyTest {

	@Autowired
	private TeamApplicationService teamApplicationService;

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

	@BeforeEach
	void setUp() {
		cleanUp();
	}

	@AfterEach
	void tearDown() {
		cleanUp();
	}

	private void cleanUp() {
		teamApplicationRepository.deleteAll();
		userCategoryMembershipRepository.deleteAll();
		teamMemberRepository.deleteAll();
		teamRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void approveApplication_allowsOnlyOneSuccessWhenSameApplicationIsApprovedConcurrently() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		User applicant = userRepository.save(User.create("applicant@test.com", "encoded-password", "applicant"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		TeamApplication application = teamApplicationRepository.save(
			TeamApplication.create(team, applicant, "message")
		);

		ConcurrentResult result = runConcurrently(5,
			() -> teamApplicationService.approveApplication(application.getId(), owner.getId()));

		TeamApplication approvedApplication = teamApplicationRepository.findById(application.getId()).orElseThrow();
		assertThat(result.successCount()).isEqualTo(1);
		assertThat(result.failureCount()).isEqualTo(4);
		assertThat(approvedApplication.getStatus()).isEqualTo(TeamApplicationStatus.APPROVED);
		assertThat(teamMemberRepository.countByTeamIdAndUserId(team.getId(), applicant.getId())).isEqualTo(1);
	}

	@Test
	void processApplication_allowsOnlyOneSuccessWhenApproveAndRejectRunConcurrently() throws Exception {
		User owner = userRepository.save(User.create("owner@test.com", "encoded-password", "owner"));
		User applicant = userRepository.save(User.create("applicant@test.com", "encoded-password", "applicant"));
		Team team = teamRepository.save(Team.create("Futsal Team", null, "FUTSAL", owner));
		TeamApplication application = teamApplicationRepository.save(
			TeamApplication.create(team, applicant, "message")
		);

		ConcurrentResult result = runConcurrently(List.of(
			() -> teamApplicationService.approveApplication(application.getId(), owner.getId()),
			() -> teamApplicationService.rejectApplication(application.getId(), owner.getId())
		));

		TeamApplication processedApplication = teamApplicationRepository.findById(application.getId()).orElseThrow();
		long memberCount = teamMemberRepository.countByTeamIdAndUserId(team.getId(), applicant.getId());

		assertThat(result.successCount()).isEqualTo(1);
		assertThat(result.failureCount()).isEqualTo(1);
		assertThat(processedApplication.getStatus())
			.isIn(TeamApplicationStatus.APPROVED, TeamApplicationStatus.REJECTED);
		if (processedApplication.getStatus() == TeamApplicationStatus.APPROVED) {
			assertThat(memberCount).isEqualTo(1);
		}
		else {
			assertThat(memberCount).isZero();
		}
	}

	@Test
	void approveApplication_allowsOnlyOneSameCategoryApplicationWhenDifferentTeamsAreApprovedConcurrently() throws Exception {
		User owner1 = userRepository.save(User.create("owner1@test.com", "encoded-password", "owner1"));
		User owner2 = userRepository.save(User.create("owner2@test.com", "encoded-password", "owner2"));
		User applicant = userRepository.save(User.create("applicant@test.com", "encoded-password", "applicant"));
		Team team1 = teamRepository.save(Team.create("Futsal Team 1", null, "FUTSAL", owner1));
		Team team2 = teamRepository.save(Team.create("Futsal Team 2", null, "FUTSAL", owner2));
		TeamApplication application1 = teamApplicationRepository.save(
			TeamApplication.create(team1, applicant, "first")
		);
		TeamApplication application2 = teamApplicationRepository.save(
			TeamApplication.create(team2, applicant, "second")
		);

		ConcurrentResult result = runConcurrently(List.of(
			() -> teamApplicationService.approveApplication(application1.getId(), owner1.getId()),
			() -> teamApplicationService.approveApplication(application2.getId(), owner2.getId())
		));

		TeamApplication processedApplication1 = teamApplicationRepository.findById(application1.getId()).orElseThrow();
		TeamApplication processedApplication2 = teamApplicationRepository.findById(application2.getId()).orElseThrow();
		long team1MemberCount = teamMemberRepository.countByTeamIdAndUserId(team1.getId(), applicant.getId());
		long team2MemberCount = teamMemberRepository.countByTeamIdAndUserId(team2.getId(), applicant.getId());
		long approvedCount = List.of(processedApplication1, processedApplication2)
			.stream()
			.filter(application -> application.getStatus() == TeamApplicationStatus.APPROVED)
			.count();

		assertThat(result.successCount()).isEqualTo(1);
		assertThat(result.failureCount()).isEqualTo(1);
		assertThat(approvedCount).isEqualTo(result.successCount());
		assertThat(team1MemberCount + team2MemberCount).isEqualTo(result.successCount());
		assertThat(userCategoryMembershipRepository.countByUserIdAndCategory(applicant.getId(), "FUTSAL"))
			.isEqualTo(1);
	}

	private ConcurrentResult runConcurrently(int taskCount, Callable<TeamApplicationResponse> task) throws Exception {
		List<Callable<TeamApplicationResponse>> tasks = new ArrayList<>();
		for (int i = 0; i < taskCount; i++) {
			tasks.add(task);
		}
		return runConcurrently(tasks);
	}

	private ConcurrentResult runConcurrently(List<Callable<TeamApplicationResponse>> tasks) throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(tasks.size());
		CountDownLatch ready = new CountDownLatch(tasks.size());
		CountDownLatch start = new CountDownLatch(1);
		Queue<TeamApplicationResponse> successes = new ConcurrentLinkedQueue<>();
		Queue<Throwable> failures = new ConcurrentLinkedQueue<>();

		try {
			List<Future<Object>> futures = tasks.stream()
				.map(task -> executorService.submit(() -> {
					ready.countDown();
					start.await();
					try {
						successes.add(task.call());
					}
					catch (Throwable throwable) {
						failures.add(throwable);
					}
					return null;
				}))
				.toList();

			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			for (Future<Object> future : futures) {
				future.get(10, TimeUnit.SECONDS);
			}
		}
		finally {
			executorService.shutdownNow();
		}

		return new ConcurrentResult(successes.size(), failures.size(), List.copyOf(failures));
	}

	private record ConcurrentResult(int successCount, int failureCount, List<Throwable> failures) {
	}
}
