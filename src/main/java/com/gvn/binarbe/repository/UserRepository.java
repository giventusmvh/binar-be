package com.gvn.binarbe.repository;

import com.gvn.binarbe.entity.User;
import com.gvn.binarbe.enums.UserType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  List<User> findByUserType(UserType userType);

  List<User> findByBranchId(Long branchId);

  @Query(
      "SELECT u FROM User u LEFT JOIN FETCH u.roles LEFT JOIN FETCH u.branch WHERE u.email = :email")
  Optional<User> findByEmailWithRoles(@Param("email") String email);

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles LEFT JOIN FETCH u.profile WHERE u.id = :id")
  Optional<User> findByIdWithRolesAndProfile(@Param("id") Long id);

  @Query("SELECT u FROM User u WHERE u.userType = :userType AND u.branch.id = :branchId")
  List<User> findByUserTypeAndBranchId(
      @Param("userType") UserType userType, @Param("branchId") Long branchId);
}
