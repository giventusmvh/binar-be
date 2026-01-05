package com.gvn.binarbe.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for selecting a plafond/product as credit limit. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectPlafondRequest {

  @NotNull(message = "Product ID is required") private Long productId;
}
