package com.gvn.binarbe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBranchRequest {

  @NotBlank(message = "Branch code is required")
  @Size(min = 3, max = 20, message = "Branch code must be between 3 and 20 characters")
  private String code;

  @NotBlank(message = "Location is required")
  @Size(min = 3, max = 100, message = "Location must be between 5 and 100 characters")
  private String location;
}
