package com.neomango.auth.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.neomango.global.security.AuthenticatedUser;
import com.neomango.user.entity.UserRole;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";
	private static final String ROLE_PREFIX = "ROLE_";

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String token = resolveToken(request);

		if (token != null) {
			authenticate(token);
		}

		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return null;
		}

		return authorizationHeader.substring(BEARER_PREFIX.length());
	}

	private void authenticate(String token) {
		try {
			if (!jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isAccessToken(token)) {
				SecurityContextHolder.clearContext();
				return;
			}

			Long userId = jwtTokenProvider.getUserId(token);
			UserRole role = jwtTokenProvider.getRole(token);
			AuthenticatedUser principal = new AuthenticatedUser(userId, null, role.name());
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role.name()))
			);

			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (RuntimeException e) {
			SecurityContextHolder.clearContext();
		}
	}
}
