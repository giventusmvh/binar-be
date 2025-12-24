package com.gvn.binarbe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for user plafond details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPlafondResponse {

    private Long id;
    private ProductResponse product;
    private BigDecimal originalAmount;
    private BigDecimal remainingAmount;
    private LocalDateTime assignedAt;
    private Boolean isActive;
}
