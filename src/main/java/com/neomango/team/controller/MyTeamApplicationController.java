package com.neomango.team.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.global.response.ApiResponse;
import com.neomango.global.security.AuthenticatedUser;
import com.neomango.team.dto.TeamApplicationSummaryResponse;
import com.neomango.team.service.TeamApplicationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/team-applications")
public class MyTeamApplicationController {

	private final TeamApplicationService teamApplicationService;

	@GetMapping
	public ApiResponse<List<TeamApplicationSummaryResponse>> getMyTeamApplications(
		@AuthenticationPrincipal AuthenticatedUser currentUser
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return ApiResponse.success(teamApplicationService.getMyApplications(currentUser.userId()));
	}
}
