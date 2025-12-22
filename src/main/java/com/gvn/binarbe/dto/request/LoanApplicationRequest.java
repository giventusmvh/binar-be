package com.gvn.binarbe.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for submitting a loan application.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Branch ID is required")
    private Long branchId;
}
