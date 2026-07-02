package com.neomango.auth.dto;

public record AvailabilityCheckResult(
	boolean available,
	String message
) {
}
