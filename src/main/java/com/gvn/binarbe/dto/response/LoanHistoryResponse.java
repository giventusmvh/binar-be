package com.gvn.binarbe.dto.response;

import com.gvn.binarbe.enums.LoanStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for loan application history entry. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanHistoryResponse {

  private Long id;
  private String approvedByName;
  private String approvedByRole;
  private String approvedByBranch;
  private LoanStatus status;
  private String note;
  private LocalDateTime createdAt;
}
