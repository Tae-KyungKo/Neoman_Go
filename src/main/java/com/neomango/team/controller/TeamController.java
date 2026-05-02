package com.neomango.team.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.global.response.ApiResponse;
import com.neomango.global.security.AuthenticatedUser;
import com.neomango.team.service.TeamService;
import com.neomango.team.dto.TeamCreateRequest;
import com.neomango.team.dto.TeamResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams")
public class TeamController {

	private final TeamService teamService;

	@PostMapping
	public ApiResponse<TeamResponse> create(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@Valid @RequestBody TeamCreateRequest request
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return ApiResponse.success(teamService.create(currentUser.userId(), request));
	}
}

