package com.gvn.binarbe.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for role details. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {

  private Long id;
  private String name;
  private List<PermissionResponse> permissions;
}
