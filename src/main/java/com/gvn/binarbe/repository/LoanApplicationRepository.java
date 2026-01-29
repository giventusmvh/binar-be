package com.gvn.binarbe.repository;

import com.gvn.binarbe.entity.LoanApplication;
import com.gvn.binarbe.enums.LoanStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

  List<LoanApplication> findByCustomerId(Long customerId);

  List<LoanApplication> findByBranchId(Long branchId);

  List<LoanApplication> findByStatus(LoanStatus status);

  @Query("SELECT la FROM LoanApplication la WHERE la.status = :status AND la.branch.id = :branchId")
  List<LoanApplication> findByStatusAndBranchId(
      @Param("status") LoanStatus status, @Param("branchId") Long branchId);

  @Query(
      "SELECT la FROM LoanApplication la LEFT JOIN FETCH la.product LEFT JOIN FETCH la.branch LEFT JOIN FETCH la.customer WHERE la.id = :id")
  Optional<LoanApplication> findByIdWithDetails(@Param("id") Long id);

  @Query(
      "SELECT la FROM LoanApplication la LEFT JOIN FETCH la.product LEFT JOIN FETCH la.branch WHERE la.customer.id = :customerId ORDER BY la.createdAt DESC")
  List<LoanApplication> findByCustomerIdWithDetails(@Param("customerId") Long customerId);

  @Query(
      "SELECT la FROM LoanApplication la LEFT JOIN FETCH la.product LEFT JOIN FETCH la.branch LEFT JOIN FETCH la.customer WHERE la.status = :status ORDER BY la.createdAt ASC")
  List<LoanApplication> findByStatusWithDetails(@Param("status") LoanStatus status);

  @Query(
      "SELECT la FROM LoanApplication la LEFT JOIN FETCH la.product LEFT JOIN FETCH la.branch LEFT JOIN FETCH la.customer WHERE la.status = :status AND la.branch.id = :branchId ORDER BY la.createdAt ASC")
  List<LoanApplication> findByStatusAndBranchIdWithDetails(
      @Param("status") LoanStatus status, @Param("branchId") Long branchId);

  @Query(
      "SELECT la FROM LoanApplication la LEFT JOIN FETCH la.product LEFT JOIN FETCH la.branch LEFT JOIN FETCH la.customer ORDER BY la.createdAt DESC")
  List<LoanApplication> findAllWithDetails();

  /**
   * Check if customer has any pending loan applications. Used to prevent multiple simultaneous loan
   * submissions.
   */
  boolean existsByCustomerIdAndStatusIn(Long customerId, List<LoanStatus> statuses);
}
