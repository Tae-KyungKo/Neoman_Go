package com.neomango.team.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neomango.team.entity.Team;
import com.neomango.team.dto.TeamCreateRequest;
import com.neomango.team.dto.TeamResponse;
import com.neomango.team.repository.TeamRepository;
import com.neomango.user.service.UserService;
import com.neomango.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

	private final TeamRepository teamRepository;
	private final UserService userService;

	public TeamResponse create(Long ownerId, TeamCreateRequest request) {
		User owner = userService.getById(ownerId);
		Team team = Team.create(request.name(), request.capacity(), owner);
		return TeamResponse.from(teamRepository.save(team));
	}
}

