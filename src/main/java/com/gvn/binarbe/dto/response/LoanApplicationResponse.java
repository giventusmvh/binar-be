package com.gvn.binarbe.dto.response;

import com.gvn.binarbe.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for loan application details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponse {

    private Long id;
    private String customerName;
    private String customerEmail;
    private ProductResponse product; // The plafond/product used
    private BranchResponse branch;
    private BigDecimal requestedAmount;
    private Integer requestedTenor;
    private BigDecimal requestedRate;
    private LoanStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
