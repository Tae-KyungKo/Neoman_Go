package com.neomango.auth.jwt;

import org.springframework.stereotype.Component;

import com.neomango.auth.dto.TokenResponse;
import com.neomango.user.entity.User;

@Component
public class JwtTokenProvider {

	public TokenResponse createToken(User user) {
		return new TokenResponse("", "", "Bearer");
	}
}

