package com.gvn.binarbe.repository;

import com.gvn.binarbe.entity.Branch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

  Optional<Branch> findByCode(String code);

  boolean existsByCode(String code);
}
