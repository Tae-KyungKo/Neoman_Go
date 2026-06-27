package com.neomango.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neomango.team.entity.UserCategoryMembership;

public interface UserCategoryMembershipRepository extends JpaRepository<UserCategoryMembership, Long> {

	boolean existsByUserIdAndCategory(Long userId, String category);

	long countByUserIdAndCategory(Long userId, String category);

	long deleteByUserIdAndCategory(Long userId, String category);
}
