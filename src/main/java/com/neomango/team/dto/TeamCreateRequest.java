package com.neomango.team.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeamCreateRequest(
	@NotBlank
	@Size(max = 50)
	String name,

	@Size(max = 500)
	String description,

	@NotBlank
	@Size(max = 50)
	String category,

	@NotNull
	@Min(2)
	Integer maxMemberCount
) {
}
