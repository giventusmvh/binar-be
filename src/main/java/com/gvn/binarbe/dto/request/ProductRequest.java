package com.gvn.binarbe.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

  @NotBlank(message = "Product name is required")
  private String name;

  @NotNull(message = "Amount is required") @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
  private BigDecimal amount;

  @NotNull(message = "Tenor is required") @Min(value = 1, message = "Tenor must be at least 1 month")
  private Integer tenor;

  @NotNull(message = "Interest rate is required") @DecimalMin(value = "0.0", inclusive = false, message = "Interest rate must be greater than 0")
  private BigDecimal interestRate;
}
