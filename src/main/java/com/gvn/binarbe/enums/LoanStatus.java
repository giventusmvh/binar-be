package com.gvn.binarbe.enums;

/**
 * Enum representing the status of a loan application through the approval workflow.
 *
 * <p>Flow: SUBMITTED -> MARKETING_APPROVED -> BRANCH_MANAGER_APPROVED -> APPROVED Any stage can
 * result in REJECTED
 */
public enum LoanStatus {
  SUBMITTED, // Initial status when customer submits
  MARKETING_APPROVED, // Approved by Marketing
  MARKETING_REJECTED, // Rejected by Marketing
  BRANCH_MANAGER_APPROVED, // Approved by Branch Manager
  BRANCH_MANAGER_REJECTED, // Rejected by Branch Manager
  APPROVED, // Final approval by Backoffice
  REJECTED // Final rejection
}
