package com.neomango.team.dto;

import jakarta.validation.constraints.NotNull;

public record OwnerDelegationRequest(
	@NotNull
	Long targetTeamMemberId
) {
}
