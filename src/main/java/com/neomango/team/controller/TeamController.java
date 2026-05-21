package com.neomango.team.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.global.response.ApiResponse;
import com.neomango.global.security.AuthenticatedUser;
import com.neomango.team.dto.TeamApplicationCreateRequest;
import com.neomango.team.dto.TeamApplicationOwnerResponse;
import com.neomango.team.dto.TeamApplicationResponse;
import com.neomango.team.dto.TeamCreateRequest;
import com.neomango.team.dto.TeamDetailResponse;
import com.neomango.team.dto.TeamResponse;
import com.neomango.team.dto.TeamSummaryResponse;
import com.neomango.team.service.TeamApplicationService;
import com.neomango.team.service.TeamService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams")
public class TeamController {

	private final TeamService teamService;
	private final TeamApplicationService teamApplicationService;

	@GetMapping
	public ApiResponse<Page<TeamSummaryResponse>> getTeams(
		@RequestParam(required = false) String category,
		@PageableDefault(size = 20)
		@SortDefault.SortDefaults({
			@SortDefault(sort = "createdAt", direction = Sort.Direction.DESC),
			@SortDefault(sort = "id", direction = Sort.Direction.DESC)
		}) Pageable pageable
	) {
		return ApiResponse.success(teamService.getTeams(category, pageable));
	}

	@GetMapping("/{teamId}")
	public ApiResponse<TeamDetailResponse> getTeamDetail(@PathVariable Long teamId) {
		return ApiResponse.success(teamService.getTeamDetail(teamId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<TeamResponse> createTeam(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@Valid @RequestBody TeamCreateRequest request
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return ApiResponse.success(teamService.createTeam(currentUser.userId(), request));
	}

	@PostMapping("/{teamId}/applications")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<TeamApplicationResponse> createTeamApplication(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable Long teamId,
		@Valid @RequestBody TeamApplicationCreateRequest request
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return ApiResponse.success(teamApplicationService.createApplication(teamId, currentUser.userId(), request));
	}

	@GetMapping("/{teamId}/applications")
	public ApiResponse<List<TeamApplicationOwnerResponse>> getTeamApplications(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable Long teamId
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return ApiResponse.success(teamApplicationService.getPendingApplicationsForOwner(teamId, currentUser.userId()));
	}

	@PatchMapping("/{teamId}/close")
	public ApiResponse<Void> closeTeam(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable Long teamId
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		teamService.closeTeam(currentUser.userId(), teamId);
		return ApiResponse.successWithoutData();
	}

	@DeleteMapping("/{teamId}")
	public ApiResponse<Void> deleteTeam(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable Long teamId
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		teamService.deleteTeam(currentUser.userId(), teamId);
		return ApiResponse.successWithoutData();
	}
}
