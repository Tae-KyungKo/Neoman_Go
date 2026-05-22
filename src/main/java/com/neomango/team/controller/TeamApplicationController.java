package com.neomango.team.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.global.response.ApiResponse;
import com.neomango.global.security.AuthenticatedUser;
import com.neomango.team.dto.TeamApplicationResponse;
import com.neomango.team.service.TeamApplicationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/team-applications")
public class TeamApplicationController {

	private final TeamApplicationService teamApplicationService;

	@PatchMapping("/{applicationId}/cancel")
	public ApiResponse<TeamApplicationResponse> cancelTeamApplication(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable Long applicationId
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return ApiResponse.success(teamApplicationService.cancelApplication(applicationId, currentUser.userId()));
	}

	@PostMapping("/{applicationId}/approve")
	public ApiResponse<TeamApplicationResponse> approveTeamApplication(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable Long applicationId
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return ApiResponse.success(teamApplicationService.approveApplication(applicationId, currentUser.userId()));
	}

	@PostMapping("/{applicationId}/reject")
	public ApiResponse<TeamApplicationResponse> rejectTeamApplication(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable Long applicationId
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return ApiResponse.success(teamApplicationService.rejectApplication(applicationId, currentUser.userId()));
	}
}
