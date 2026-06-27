package com.neomango.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByRole(UserRole role);
}

