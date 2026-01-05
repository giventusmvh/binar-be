package com.gvn.binarbe.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for assigning permissions to a role. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignPermissionRequest {

  @NotEmpty(message = "Permission IDs are required")
  private Set<Long> permissionIds;
}
