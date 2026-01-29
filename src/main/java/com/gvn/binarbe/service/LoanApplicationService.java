package com.gvn.binarbe.service;

import com.gvn.binarbe.dto.request.LoanApplicationRequest;
import com.gvn.binarbe.dto.response.LoanApplicationResponse;
import com.gvn.binarbe.dto.response.LoanHistoryResponse;
import java.util.List;

/** Service interface for loan application operations. */
public interface LoanApplicationService {

  /**
   * Submit a new loan application.
   *
   * @param email customer email
   * @param request loan application data
   * @return created loan application
   */
  LoanApplicationResponse submitLoan(String email, LoanApplicationRequest request);

  /**
   * Get all loans for current customer.
   *
   * @param email customer email
   * @return list of loan applications
   */
  List<LoanApplicationResponse> getMyLoans(String email);

  /**
   * Get loan details by ID.
   *
   * @param email customer email
   * @param loanId loan application ID
   * @return loan application details
   */
  LoanApplicationResponse getLoanById(String email, Long loanId);

  /**
   * Get approval history for a loan.
   *
   * @param email customer email
   * @param loanId loan application ID
   * @return list of history entries
   */
  List<LoanHistoryResponse> getLoanHistory(String email, Long loanId);

  /**
   * Get all loans in the system (Superadmin only).
   *
   * @return list of all loan applications
   */
  List<LoanApplicationResponse> getAllLoans();
}
