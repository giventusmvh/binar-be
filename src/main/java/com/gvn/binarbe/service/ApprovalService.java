package com.gvn.binarbe.service;

import com.gvn.binarbe.dto.request.ApprovalRequest;
import com.gvn.binarbe.dto.response.LoanApplicationResponse;
import com.gvn.binarbe.dto.response.MyApprovalHistoryResponse;
import java.util.List;

/** Service interface for loan approval operations. */
public interface ApprovalService {

  /**
   * Get pending loans for approval based on user role and branch.
   *
   * @param email approver email
   * @return list of pending loans
   */
  List<LoanApplicationResponse> getPendingLoans(String email);

  /**
   * Approve a loan application.
   *
   * @param email approver email
   * @param loanId loan application ID
   * @param request approval data
   * @return updated loan application
   */
  LoanApplicationResponse approve(String email, Long loanId, ApprovalRequest request);

  /**
   * Reject a loan application.
   *
   * @param email approver email
   * @param loanId loan application ID
   * @param request rejection data
   * @return updated loan application
   */
  LoanApplicationResponse reject(String email, Long loanId, ApprovalRequest request);

  /**
   * Get approval/rejection history for the current user.
   *
   * @param email user email
   * @return list of approval actions taken by user
   */
  List<MyApprovalHistoryResponse> getMyApprovalHistory(String email);
}
