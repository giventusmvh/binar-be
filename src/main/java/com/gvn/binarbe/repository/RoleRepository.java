package com.gvn.binarbe.repository;

import com.gvn.binarbe.entity.Role;
import com.gvn.binarbe.enums.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findByName(RoleName name);

  boolean existsByName(RoleName name);

  @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
  Optional<Role> findByIdWithPermissions(@Param("id") Long id);
}
