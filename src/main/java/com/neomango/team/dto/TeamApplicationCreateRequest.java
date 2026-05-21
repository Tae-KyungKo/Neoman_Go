package com.neomango.team.dto;

import jakarta.validation.constraints.Size;

public record TeamApplicationCreateRequest(
	@Size(max = 500)
	String message
) {
}
