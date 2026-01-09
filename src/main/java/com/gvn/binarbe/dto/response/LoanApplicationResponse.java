package com.gvn.binarbe.dto.response;

import com.gvn.binarbe.enums.LoanStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for loan application details. Customer data is snapshot data from submission time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponse {

  private Long id;

  // Customer snapshot data (preserved from submission time)
  private String customerName;
  private String customerEmail;
  private String customerNik;
  private String customerPhone;
  private String customerAddress;
  private LocalDate customerBirthdate;

  private String customerKtpPath;
  private String customerKkPath;
  private String customerNpwpPath;

  private ProductResponse product; // The plafond/product used
  private BranchResponse branch;
  private BigDecimal requestedAmount;
  private Integer requestedTenor;
  private BigDecimal requestedRate;
  private LoanStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
