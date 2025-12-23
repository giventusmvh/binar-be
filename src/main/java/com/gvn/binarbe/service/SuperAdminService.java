package com.gvn.binarbe.service;

import com.gvn.binarbe.dto.request.AssignPermissionRequest;
import com.gvn.binarbe.dto.request.AssignRoleRequest;
import com.gvn.binarbe.dto.request.CreateInternalUserRequest;
import com.gvn.binarbe.dto.response.PermissionResponse;
import com.gvn.binarbe.dto.response.RoleResponse;
import com.gvn.binarbe.dto.response.UserResponse;

import java.util.List;

/**
 * Service interface for superadmin operations.
 */
public interface SuperAdminService {

    /**
     * Create a new internal user with role and optional branch assignment.
     *
     * @param request internal user creation data
     * @return created user
     */
    UserResponse createInternalUser(CreateInternalUserRequest request);

    /**
     * Get all users in the system.
     *
     * @return list of users
     */
    List<UserResponse> getAllUsers();

    /**
     * Get user by ID.
     *
     * @param userId user ID
     * @return user details
     */
    UserResponse getUserById(Long userId);

    /**
     * Assign a role to a user.
     *
     * @param userId  user ID
     * @param request role assignment data
     * @return updated user
     */
    UserResponse assignRole(Long userId, AssignRoleRequest request);

    /**
     * Remove a role from a user.
     *
     * @param userId user ID
     * @param roleId role ID to remove
     * @return updated user
     */
    UserResponse removeRole(Long userId, Long roleId);

    /**
     * Get all roles.
     *
     * @return list of roles
     */
    List<RoleResponse> getAllRoles();

    /**
     * Update permissions for a role.
     *
     * @param roleId  role ID
     * @param request permission IDs to assign
     * @return updated role
     */
    RoleResponse updateRolePermissions(Long roleId, AssignPermissionRequest request);

    /**
     * Get all permissions.
     *
     * @return list of permissions
     */
    List<PermissionResponse> getAllPermissions();
}
