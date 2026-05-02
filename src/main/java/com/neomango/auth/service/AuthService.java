package com.neomango.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neomango.auth.dto.SignupRequest;
import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.user.entity.User;
import com.neomango.user.dto.UserResponse;
import com.neomango.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserResponse signup(SignupRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		User user = User.create(
			request.email(),
			passwordEncoder.encode(request.password()),
			request.nickname()
		);

		return UserResponse.from(userRepository.save(user));
	}
}

