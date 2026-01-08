package com.gvn.binarbe.repository;

import com.gvn.binarbe.entity.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

  Optional<UserProfile> findByUserId(Long userId);

  boolean existsByUserId(Long userId);

  Optional<UserProfile> findByKtpPathOrKkPathOrNpwpPath(
      String ktpPath, String kkPath, String npwpPath);
}
