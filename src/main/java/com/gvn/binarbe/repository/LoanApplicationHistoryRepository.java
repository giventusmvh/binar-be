package com.gvn.binarbe.repository;

import com.gvn.binarbe.entity.LoanApplicationHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanApplicationHistoryRepository
    extends JpaRepository<LoanApplicationHistory, Long> {

  List<LoanApplicationHistory> findByLoanApplicationIdOrderByCreatedAtAsc(Long loanApplicationId);

  @Query(
      "SELECT lah FROM LoanApplicationHistory lah LEFT JOIN FETCH lah.approvedBy WHERE lah.loanApplication.id = :loanApplicationId ORDER BY lah.createdAt ASC")
  List<LoanApplicationHistory> findByLoanApplicationIdWithApprover(
      @Param("loanApplicationId") Long loanApplicationId);

  @Query(
      """
            SELECT lah FROM LoanApplicationHistory lah
            LEFT JOIN FETCH lah.loanApplication la
            LEFT JOIN FETCH la.product
            LEFT JOIN FETCH la.branch
            WHERE lah.approvedBy.id = :userId
            ORDER BY lah.createdAt DESC
            """)
  List<LoanApplicationHistory> findByApprovedByIdWithLoanDetails(@Param("userId") Long userId);
}
