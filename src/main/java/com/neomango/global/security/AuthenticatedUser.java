package com.neomango.global.security;

public record AuthenticatedUser(
	Long userId,
	String email,
	String role
) {
}

