package com.gvn.binarbe.mapper;

import com.gvn.binarbe.dto.response.AuthResponse;
import com.gvn.binarbe.entity.User;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

  public AuthResponse toAuthResponse(User user, String token) {
    return AuthResponse.builder()
        .token(token)
        .tokenType("Bearer")
        .userId(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .roles(
            user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList()))
        .permissions(
            user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .distinct()
                .collect(Collectors.toList()))
        .build();
  }
}
