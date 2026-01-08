package com.gvn.binarbe.controller;

import com.gvn.binarbe.dto.response.UserResponse;
import com.gvn.binarbe.service.UserService;
import com.gvn.binarbe.util.ApiResponse;
import com.gvn.binarbe.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  /**
   * Get user by ID. Accessible by SUPERADMIN, MARKETING, BRANCH_MANAGER, BACKOFFICE.
   *
   * @param id user ID
   * @return user details
   */
  @GetMapping("/api/users/{id}")
  @PreAuthorize("hasAnyRole('SUPERADMIN', 'MARKETING', 'BRANCH_MANAGER', 'BACKOFFICE')")
  public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
    UserResponse response = userService.getUserById(id);
    return ResponseUtil.ok(response);
  }
}
