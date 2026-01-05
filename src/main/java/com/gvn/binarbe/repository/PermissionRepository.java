package com.gvn.binarbe.repository;

import com.gvn.binarbe.entity.Permission;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

  Optional<Permission> findByCode(String code);

  boolean existsByCode(String code);

  Set<Permission> findByIdIn(Set<Long> ids);
}
