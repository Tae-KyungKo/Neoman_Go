package com.neomango.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.user.dto.MeResponse;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;
import com.neomango.user.entity.UserStatus;
import com.neomango.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	private static final Long USER_ID = 1L;
	private static final String EMAIL = "user@test.com";
	private static final String ENCODED_PASSWORD = "encoded-password";
	private static final String NICKNAME = "nickname";

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserService userService;

	@Test
	void getCurrentUserReturnsMeResponseWhenUserIsActive() {
		User user = activeUser();
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		MeResponse response = userService.getCurrentUser(USER_ID);

		assertThat(response.id()).isEqualTo(USER_ID);
		assertThat(response.email()).isEqualTo(EMAIL);
		assertThat(response.nickname()).isEqualTo(NICKNAME);
		assertThat(response.role()).isEqualTo(UserRole.USER);
		assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void getCurrentUserThrowsExceptionWhenUserDoesNotExist() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getCurrentUser(USER_ID))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	void getCurrentUserThrowsExceptionWhenUserIsDeleted() {
		User user = activeUser();
		user.softDelete();
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> userService.getCurrentUser(USER_ID))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	void getCurrentUserThrowsExceptionWhenUserIdIsNull() {
		assertThatThrownBy(() -> userService.getCurrentUser(null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	private User activeUser() {
		User user = User.create(com.neomango.support.TestLoginIds.next(), EMAIL, ENCODED_PASSWORD, NICKNAME);
		ReflectionTestUtils.setField(user, "id", USER_ID);
		return user;
	}
}
