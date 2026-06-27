package com.neomango.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamCreateRequest(
	@NotBlank
	@Size(max = 50)
	String name,

	@Size(max = 500)
	String description,

	@NotBlank
	@Size(max = 50)
	String category
) {
}
