package com.gvn.binarbe.dto.response;

import com.gvn.binarbe.enums.LoanStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for staff's own approval/rejection history. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyApprovalHistoryResponse {

  private Long id; // History ID
  private Long loanId; // Loan ID
  private String customerName; // Customer snapshot
  private String productName; // Product name
  private BigDecimal loanAmount; // Requested amount
  private String branchLocation; // Branch
  private LoanStatus actionTaken; // Status when action was taken
  private String note;
  private LocalDateTime actionDate;
}
