package com.neomango.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamCreateRequest(
	@NotBlank
	@Size(max = 50)
	String name
) {
}

