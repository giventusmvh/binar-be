package com.gvn.binarbe.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Request DTO for assigning permissions to a role.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignPermissionRequest {

    @NotEmpty(message = "Permission IDs are required")
    private Set<Long> permissionIds;
}
