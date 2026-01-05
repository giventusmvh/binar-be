package com.gvn.binarbe.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating internal users by SuperAdmin. Allows immediate role and branch
 * assignment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInternalUserRequest {

  @NotBlank(message = "Name is required")
  @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
  private String name;

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
  private String password;

  @NotNull(message = "Role ID is required") private Long roleId;

  /**
   * Branch ID is optional for SUPERADMIN/BACKOFFICE roles, but required for
   * MARKETING/BRANCH_MANAGER roles.
   */
  private Long branchId;
}
