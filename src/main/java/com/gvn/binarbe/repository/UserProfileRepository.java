package com.gvn.binarbe.repository;

import com.gvn.binarbe.entity.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

  Optional<UserProfile> findByUserId(Long userId);

  boolean existsByUserId(Long userId);

  @org.springframework.data.jpa.repository.Query(
      "SELECT p FROM UserProfile p WHERE p.ktpPath = :filename OR p.kkPath = :filename OR p.npwpPath = :filename OR p.selfiePath = :filename OR p.salarySlipPath = :filename")
  Optional<UserProfile> findByAnyDocumentPath(
      @org.springframework.data.repository.query.Param("filename") String filename);
}
