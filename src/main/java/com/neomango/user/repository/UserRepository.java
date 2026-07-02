package com.neomango.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByLoginId(String loginId);

	Optional<User> findByEmail(String email);

	boolean existsByLoginId(String loginId);

	boolean existsByEmail(String email);

	boolean existsByNickname(String nickname);

	boolean existsByRole(UserRole role);
}

