package com.gvn.binarbe.mapper;

import com.gvn.binarbe.dto.response.*;
import com.gvn.binarbe.entity.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public UserResponse toUserResponse(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .name(user.getName())
        .email(user.getEmail())
        .userType(user.getUserType())
        .isActive(user.getIsActive())
        .branch(user.getBranch() != null ? toBranchResponse(user.getBranch()) : null)
        .profile(user.getProfile() != null ? toProfileResponse(user.getProfile()) : null)
        .roles(
            user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList()))
        .createdAt(user.getCreatedAt())
        .build();
  }

  public UserProfileResponse toProfileResponse(UserProfile profile) {
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

  public BranchResponse toBranchResponse(Branch branch) {
    return BranchResponse.builder()
        .id(branch.getId())
        .code(branch.getCode())
        .location(branch.getLocation())
        .build();
  }

  public RoleResponse toRoleResponse(Role role) {
    return RoleResponse.builder()
        .id(role.getId())
        .name(role.getName().name())
        .permissions(
            role.getPermissions().stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toList()))
        .build();
  }

  public PermissionResponse toPermissionResponse(Permission permission) {
    return PermissionResponse.builder()
        .id(permission.getId())
        .code(permission.getCode())
        .description(permission.getDescription())
        .build();
  }
}
