package com.gvn.binarbe.controller;

import com.gvn.binarbe.dto.request.AssignPermissionRequest;
import com.gvn.binarbe.dto.request.AssignRoleRequest;
import com.gvn.binarbe.dto.request.CreateInternalUserRequest;
import com.gvn.binarbe.dto.response.PermissionResponse;
import com.gvn.binarbe.dto.response.RoleResponse;
import com.gvn.binarbe.dto.response.UserResponse;
import com.gvn.binarbe.service.SuperAdminService;
import com.gvn.binarbe.util.ApiResponse;
import com.gvn.binarbe.util.ResponseUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for superadmin operations. Handles user management, role assignment, and permission
 * management.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class SuperAdminController {

  private final SuperAdminService superAdminService;

  /**
   * Create a new internal user with role and branch assignment. POST /api/admin/users Requires
   * SUPERADMIN role.
   */
  @PostMapping("/users")
  public ResponseEntity<ApiResponse<UserResponse>> createInternalUser(
      @Valid @RequestBody CreateInternalUserRequest request) {
    UserResponse response = superAdminService.createInternalUser(request);
    return ResponseUtil.created("Internal user created successfully", response);
  }

  /** Get all users in the system. GET /api/admin/users Requires SUPERADMIN role. */
  @GetMapping("/users")
  public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
    List<UserResponse> response = superAdminService.getAllUsers();
    return ResponseUtil.ok(response);
  }

  /** Get user details by ID. GET /api/admin/users/{id} Requires SUPERADMIN role. */
  @GetMapping("/users/{id}")
  public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
    UserResponse response = superAdminService.getUserById(id);
    return ResponseUtil.ok(response);
  }

  /** Assign a role to a user. POST /api/admin/users/{id}/roles Requires SUPERADMIN role. */
  @PostMapping("/users/{id}/roles")
  public ResponseEntity<ApiResponse<UserResponse>> assignRole(
      @PathVariable Long id, @Valid @RequestBody AssignRoleRequest request) {
    UserResponse response = superAdminService.assignRole(id, request);
    return ResponseUtil.ok("Role assigned successfully", response);
  }

  /**
   * Remove a role from a user. DELETE /api/admin/users/{userId}/roles/{roleId} Requires SUPERADMIN
   * role.
   */
  @DeleteMapping("/users/{userId}/roles/{roleId}")
  public ResponseEntity<ApiResponse<UserResponse>> removeRole(
      @PathVariable Long userId, @PathVariable Long roleId) {
    UserResponse response = superAdminService.removeRole(userId, roleId);
    return ResponseUtil.ok("Role removed successfully", response);
  }

  /** Get all roles. GET /api/admin/roles Requires SUPERADMIN role. */
  @GetMapping("/roles")
  public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
    List<RoleResponse> response = superAdminService.getAllRoles();
    return ResponseUtil.ok(response);
  }

  /**
   * Update permissions for a role. PUT /api/admin/roles/{id}/permissions Requires SUPERADMIN role.
   */
  @PutMapping("/roles/{id}/permissions")
  public ResponseEntity<ApiResponse<RoleResponse>> updateRolePermissions(
      @PathVariable Long id, @Valid @RequestBody AssignPermissionRequest request) {
    RoleResponse response = superAdminService.updateRolePermissions(id, request);
    return ResponseUtil.ok("Role permissions updated successfully", response);
  }

  /** Get all permissions. GET /api/admin/permissions Requires SUPERADMIN role. */
  @GetMapping("/permissions")
  public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
    List<PermissionResponse> response = superAdminService.getAllPermissions();
    return ResponseUtil.ok(response);
  }
}
