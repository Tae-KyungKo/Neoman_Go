package com.neomango.team.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.neomango.team.entity.Team;
import com.neomango.team.entity.TeamMember;
import com.neomango.team.entity.TeamMemberRole;
import com.neomango.team.entity.TeamMemberStatus;
import com.neomango.team.entity.TeamStatus;
import com.neomango.user.entity.User;
import com.neomango.user.repository.UserRepository;

import jakarta.persistence.CascadeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OneToMany;

@ActiveProfiles("test")
@DataJpaTest
class TeamRepositoryTest {

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private TeamMemberRepository teamMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("save Team")
	void saveTeam() {
		User owner = saveUser("owner1@test.com", "owner1");
		Team team = Team.create("Game Team", "Weekend game team", "GAME", 5, owner);

		Team savedTeam = teamRepository.saveAndFlush(team);

		assertThat(savedTeam.getId()).isNotNull();
		assertThat(savedTeam.getName()).isEqualTo("Game Team");
		assertThat(savedTeam.getDescription()).isEqualTo("Weekend game team");
		assertThat(savedTeam.getCategory()).isEqualTo("GAME");
		assertThat(savedTeam.getMaxMemberCount()).isEqualTo(5);
		assertThat(savedTeam.getStatus()).isEqualTo(TeamStatus.RECRUITING);
		assertThat(savedTeam.getCreatedBy().getId()).isEqualTo(owner.getId());
		assertThat(savedTeam.getCreatedAt()).isNotNull();
		assertThat(savedTeam.getUpdatedAt()).isNotNull();
		assertThat(savedTeam.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("save User and Team relation through TeamMember")
	void saveUserTeamRelationThroughTeamMember() {
		User owner = saveUser("owner2@test.com", "owner2");
		User member = saveUser("member1@test.com", "member1");
		Team team = teamRepository.saveAndFlush(Team.create("Sports Team", null, "SPORTS", 4, owner));

		TeamMember teamMember = teamMemberRepository.saveAndFlush(TeamMember.createMember(team, member));

		assertThat(teamMember.getId()).isNotNull();
		assertThat(teamMember.getTeam().getId()).isEqualTo(team.getId());
		assertThat(teamMember.getUser().getId()).isEqualTo(member.getId());
		assertThat(teamMember.getRole()).isEqualTo(TeamMemberRole.MEMBER);
		assertThat(teamMember.getStatus()).isEqualTo(TeamMemberStatus.ACTIVE);
		assertThat(teamMember.getJoinedAt()).isNotNull();
	}

	@Test
	@DisplayName("save owner TeamMember when Team is created")
	void saveOwnerTeamMemberWhenTeamCreated() {
		User owner = saveUser("owner3@test.com", "owner3");
		Team team = teamRepository.saveAndFlush(Team.create("Owner Team", null, "GAME", 3, owner));

		assertThat(team.getMembers()).hasSize(1);
		TeamMember ownerMember = team.getMembers().get(0);
		assertThat(ownerMember.getId()).isNotNull();
		assertThat(ownerMember.getUser().getId()).isEqualTo(owner.getId());
		assertThat(ownerMember.getRole()).isEqualTo(TeamMemberRole.OWNER);
		assertThat(ownerMember.getStatus()).isEqualTo(TeamMemberStatus.ACTIVE);
	}

	@Test
	@DisplayName("TeamMember team and user associations are lazy")
	void teamMemberAssociationsAreLazy() {
		User owner = saveUser("owner4@test.com", "owner4");
		Team team = teamRepository.saveAndFlush(Team.create("Lazy Team", null, "GAME", 3, owner));
		Long teamMemberId = team.getMembers().get(0).getId();
		entityManager.clear();

		TeamMember teamMember = teamMemberRepository.findById(teamMemberId).orElseThrow();

		assertThat(Hibernate.isInitialized(teamMember.getTeam())).isFalse();
		assertThat(Hibernate.isInitialized(teamMember.getUser())).isFalse();
	}

	@Test
	@DisplayName("fail to save duplicate TeamMember for same team and user")
	void duplicateTeamMemberFails() {
		User owner = saveUser("owner5@test.com", "owner5");
		Team team = teamRepository.saveAndFlush(Team.create("Duplicate Guard Team", null, "GAME", 3, owner));

		assertThatThrownBy(() -> {
			teamMemberRepository.saveAndFlush(TeamMember.createMember(team, owner));
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("currentMemberCount starts at 1 including owner")
	void currentMemberCountStartsWithOwner() {
		User owner = saveUser("owner6@test.com", "owner6");

		Team team = teamRepository.saveAndFlush(Team.create("Count Team", null, "GAME", 6, owner));

		assertThat(team.getCurrentMemberCount()).isEqualTo(1);
		assertThat(team.getMembers()).hasSize(1);
	}

	@Test
	@DisplayName("Team save cascades owner TeamMember after flush and clear")
	void teamSaveCascadesOwnerTeamMemberAfterFlushAndClear() {
		User owner = saveUser("owner7@test.com", "owner7");
		Team team = Team.create("Cascade Team", null, "GAME", 4, owner);

		Team savedTeam = teamRepository.save(team);
		entityManager.flush();
		entityManager.clear();

		Team foundTeam = teamRepository.findById(savedTeam.getId()).orElseThrow();
		assertThat(foundTeam.getCurrentMemberCount()).isEqualTo(1);
		assertThat(foundTeam.getMembers()).hasSize(1);
		assertThat(teamMemberRepository.count()).isEqualTo(1);

		TeamMember ownerMember = teamMemberRepository.findAll().get(0);
		assertThat(ownerMember.getTeam().getId()).isEqualTo(foundTeam.getId());
		assertThat(ownerMember.getUser().getId()).isEqualTo(owner.getId());
		assertThat(ownerMember.getRole()).isEqualTo(TeamMemberRole.OWNER);
	}

	@Test
	@DisplayName("Team members association uses cascade persist")
	void teamMembersAssociationUsesCascadePersist() throws NoSuchFieldException {
		OneToMany oneToMany = Team.class.getDeclaredField("members").getAnnotation(OneToMany.class);

		assertThat(oneToMany).isNotNull();
		assertThat(Arrays.asList(oneToMany.cascade())).contains(CascadeType.ALL);
	}

	private User saveUser(String email, String nickname) {
		return userRepository.saveAndFlush(User.create(email, "encoded-password", nickname));
	}
}
