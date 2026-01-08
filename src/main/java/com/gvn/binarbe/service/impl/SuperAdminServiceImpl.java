package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.dto.request.AssignPermissionRequest;
import com.gvn.binarbe.dto.request.AssignRoleRequest;
import com.gvn.binarbe.dto.request.CreateInternalUserRequest;
import com.gvn.binarbe.dto.request.UpdateUserRequest;
import com.gvn.binarbe.dto.request.UpdateUserStatusRequest;
import com.gvn.binarbe.dto.response.*;
import com.gvn.binarbe.entity.Branch;
import com.gvn.binarbe.entity.Permission;
import com.gvn.binarbe.entity.Role;
import com.gvn.binarbe.entity.User;
import com.gvn.binarbe.enums.RoleName;
import com.gvn.binarbe.enums.UserType;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.repository.BranchRepository;
import com.gvn.binarbe.repository.PermissionRepository;
import com.gvn.binarbe.repository.RoleRepository;
import com.gvn.binarbe.repository.UserRepository;
import com.gvn.binarbe.service.SuperAdminService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of SuperAdminService for admin operations. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminServiceImpl implements SuperAdminService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final BranchRepository branchRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public UserResponse createInternalUser(CreateInternalUserRequest request) {
    log.info("Creating internal user with email: {}", request.getEmail());

    // Validate email uniqueness
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      throw BusinessException.conflict("Email already exists");
    }

    // Get and validate role
    Role role =
        roleRepository
            .findById(request.getRoleId())
            .orElseThrow(() -> BusinessException.notFound("Role not found"));

    // Cannot assign CUSTOMER role to internal user
    if (role.getName() == RoleName.CUSTOMER) {
      throw BusinessException.badRequest("Cannot assign CUSTOMER role to internal user");
    }

    // Validate branch requirement based on role
    Branch branch = null;
    boolean branchRequired =
        role.getName() == RoleName.MARKETING || role.getName() == RoleName.BRANCH_MANAGER;

    if (branchRequired && request.getBranchId() == null) {
      throw BusinessException.badRequest("Branch is required for " + role.getName() + " role");
    }

    if (request.getBranchId() != null) {
      branch =
          branchRepository
              .findById(request.getBranchId())
              .orElseThrow(() -> BusinessException.notFound("Branch not found"));
    }

    // Create user
    Set<Role> roles = new HashSet<>();
    roles.add(role);

    User user =
        User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .userType(UserType.INTERNAL)
            .isActive(true)
            .branch(branch)
            .roles(roles)
            .build();

    user = userRepository.save(user);
    log.info("Internal user created successfully with ID: {}", user.getId());

    return mapToUserResponse(user);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserResponse> getAllUsers() {
    return userRepository.findAll().stream()
        .map(this::mapToUserResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponse getUserById(Long userId) {
    User user =
        userRepository
            .findByIdWithRolesAndProfile(userId)
            .orElseThrow(() -> BusinessException.notFound("User not found"));
    return mapToUserResponse(user);
  }

  @Override
  @Transactional
  public UserResponse assignRole(Long userId, AssignRoleRequest request) {
    log.info("Assigning role {} to user {}", request.getRoleId(), userId);

    User user =
        userRepository
            .findByIdWithRolesAndProfile(userId)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    Role role =
        roleRepository
            .findById(request.getRoleId())
            .orElseThrow(() -> BusinessException.notFound("Role not found"));

    // Check if user already has this role
    if (user.getRoles().contains(role)) {
      throw BusinessException.conflict("User already has this role");
    }

    user.getRoles().add(role);
    user = userRepository.save(user);

    log.info("Role {} assigned to user {}", role.getName(), userId);

    return mapToUserResponse(user);
  }

  @Override
  @Transactional
  public UserResponse removeRole(Long userId, Long roleId) {
    log.info("Removing role {} from user {}", roleId, userId);

    User user =
        userRepository
            .findByIdWithRolesAndProfile(userId)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> BusinessException.notFound("Role not found"));

    // Check if user has this role
    if (!user.getRoles().contains(role)) {
      throw BusinessException.badRequest("User doesn't have this role");
    }

    user.getRoles().remove(role);
    user = userRepository.save(user);

    log.info("Role {} removed from user {}", role.getName(), userId);

    return mapToUserResponse(user);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RoleResponse> getAllRoles() {
    return roleRepository.findAll().stream()
        .map(this::mapToRoleResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public RoleResponse updateRolePermissions(Long roleId, AssignPermissionRequest request) {
    log.info("Updating permissions for role {}", roleId);

    Role role =
        roleRepository
            .findByIdWithPermissions(roleId)
            .orElseThrow(() -> BusinessException.notFound("Role not found"));

    Set<Permission> permissions =
        new HashSet<>(permissionRepository.findByIdIn(request.getPermissionIds()));

    if (permissions.size() != request.getPermissionIds().size()) {
      throw BusinessException.badRequest("Some permission IDs are invalid");
    }

    role.setPermissions(permissions);
    role = roleRepository.save(role);

    log.info("Permissions updated for role {}", role.getName());

    return mapToRoleResponse(role);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PermissionResponse> getAllPermissions() {
    return permissionRepository.findAll().stream()
        .map(this::mapToPermissionResponse)
        .collect(Collectors.toList());
  }

  private UserResponse mapToUserResponse(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .name(user.getName())
        .email(user.getEmail())
        .userType(user.getUserType())
        .isActive(user.getIsActive())
        .branch(user.getBranch() != null ? mapToBranchResponse(user.getBranch()) : null)
        .profile(user.getProfile() != null ? mapToProfileResponse(user.getProfile()) : null)
        .roles(
            user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList()))
        .createdAt(user.getCreatedAt())
        .build();
  }

  private UserProfileResponse mapToProfileResponse(com.gvn.binarbe.entity.UserProfile profile) {
    return UserProfileResponse.builder()
        .id(profile.getId())
        .birthdate(profile.getBirthdate())
        .phone(profile.getPhone())
        .address(profile.getAddress())
        .nik(profile.getNik())
        .isComplete(profile.isComplete())
        .ktpUrl(profile.getKtpPath() != null ? "/uploads/" + profile.getKtpPath() : null)
        .kkUrl(profile.getKkPath() != null ? "/uploads/" + profile.getKkPath() : null)
        .npwpUrl(profile.getNpwpPath() != null ? "/uploads/" + profile.getNpwpPath() : null)
        .build();
  }

  private BranchResponse mapToBranchResponse(com.gvn.binarbe.entity.Branch branch) {
    return BranchResponse.builder()
        .id(branch.getId())
        .code(branch.getCode())
        .location(branch.getLocation())
        .build();
  }

  private RoleResponse mapToRoleResponse(Role role) {
    return RoleResponse.builder()
        .id(role.getId())
        .name(role.getName().name())
        .permissions(
            role.getPermissions().stream()
                .map(this::mapToPermissionResponse)
                .collect(Collectors.toList()))
        .build();
  }

  private PermissionResponse mapToPermissionResponse(Permission permission) {
    return PermissionResponse.builder()
        .id(permission.getId())
        .code(permission.getCode())
        .description(permission.getDescription())
        .build();
  }

  @Override
  @Transactional
  public UserResponse updateUser(Long userId, UpdateUserRequest request) {
    log.info("Updating user with ID: {}", userId);

    User user =
        userRepository
            .findByIdWithRolesAndProfile(userId)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    // Update name if provided
    if (request.getName() != null && !request.getName().isBlank()) {
      user.setName(request.getName());
    }

    // Update email if provided and different from current
    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      if (!request.getEmail().equals(user.getEmail())) {
        // Check if new email is already in use
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
          throw BusinessException.conflict("Email already exists");
        }
        user.setEmail(request.getEmail());
      }
    }

    // Update branch if provided
    if (request.getBranchId() != null) {
      Branch branch =
          branchRepository
              .findById(request.getBranchId())
              .orElseThrow(() -> BusinessException.notFound("Branch not found"));
      user.setBranch(branch);
    }

    user = userRepository.save(user);
    log.info("User {} updated successfully", userId);

    return mapToUserResponse(user);
  }

  @Override
  @Transactional
  public UserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
    log.info("Updating status for user ID: {} to isActive: {}", userId, request.getIsActive());

    User user =
        userRepository
            .findByIdWithRolesAndProfile(userId)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    user.setIsActive(request.getIsActive());
    user = userRepository.save(user);

    log.info("User {} status updated to isActive: {}", userId, request.getIsActive());

    return mapToUserResponse(user);
  }
}
