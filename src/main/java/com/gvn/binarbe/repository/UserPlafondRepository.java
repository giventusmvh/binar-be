package com.gvn.binarbe.repository;

import com.gvn.binarbe.entity.UserPlafond;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserPlafond entity operations.
 */
@Repository
public interface UserPlafondRepository extends JpaRepository<UserPlafond, Long> {

    /**
     * Find active plafond for a user.
     */
    Optional<UserPlafond> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * Find any plafond (active or inactive) for a user.
     */
    Optional<UserPlafond> findByUserId(Long userId);

    /**
     * Check if user has an active plafond.
     */
    boolean existsByUserIdAndIsActiveTrue(Long userId);

    /**
     * Find active plafond with product details eagerly loaded.
     */
    @Query("SELECT up FROM UserPlafond up " +
            "JOIN FETCH up.product " +
            "WHERE up.user.id = :userId AND up.isActive = true")
    Optional<UserPlafond> findByUserIdWithProduct(@Param("userId") Long userId);
}
