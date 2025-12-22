package com.gvn.binarbe.repository;

import com.gvn.binarbe.entity.LoanApplicationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanApplicationHistoryRepository extends JpaRepository<LoanApplicationHistory, Long> {

    List<LoanApplicationHistory> findByLoanApplicationIdOrderByCreatedAtAsc(Long loanApplicationId);

    @Query("SELECT lah FROM LoanApplicationHistory lah LEFT JOIN FETCH lah.approvedBy WHERE lah.loanApplication.id = :loanApplicationId ORDER BY lah.createdAt ASC")
    List<LoanApplicationHistory> findByLoanApplicationIdWithApprover(
            @Param("loanApplicationId") Long loanApplicationId);
}
