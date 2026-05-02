package com.neomango.team.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamCreateRequest(
	@NotBlank
	@Size(max = 50)
	String name,

	@Min(2)
	@Max(100)
	int capacity
) {
}

