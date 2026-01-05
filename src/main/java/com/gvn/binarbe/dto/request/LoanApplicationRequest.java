package com.gvn.binarbe.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for submitting a loan application. Product is derived from user's active plafond. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationRequest {

  @NotNull(message = "Branch ID is required") private Long branchId;

  @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") private BigDecimal amount;

  @NotNull(message = "Tenor is required") @Min(value = 1, message = "Tenor must be at least 1 month")
  private Integer tenor;

  @NotNull(message = "Interest rate is required") @Positive(message = "Interest rate must be positive") private BigDecimal interestRate;
}
