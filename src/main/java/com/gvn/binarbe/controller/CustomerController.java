package com.gvn.binarbe.controller;

import com.gvn.binarbe.dto.request.SelectPlafondRequest;
import com.gvn.binarbe.dto.request.UpdateProfileRequest;
import com.gvn.binarbe.dto.response.BranchResponse;
import com.gvn.binarbe.dto.response.UserPlafondResponse;
import com.gvn.binarbe.dto.response.UserProfileResponse;
import com.gvn.binarbe.dto.response.UserResponse;
import com.gvn.binarbe.service.CustomerService;
import com.gvn.binarbe.service.PlafondService;
import com.gvn.binarbe.util.ApiResponse;
import com.gvn.binarbe.util.ResponseUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for customer operations. Handles profile management, plafond selection, and browsing
 * products/branches. Access controlled by permissions assigned to roles.
 */
@RestController
@RequiredArgsConstructor
public class CustomerController {

  private final CustomerService customerService;
  private final PlafondService plafondService;

  /** Get current customer profile. GET /api/customer/profile Requires PROFILE_READ permission. */
  @GetMapping("/api/customer/profile")
  @PreAuthorize("hasAuthority('PROFILE_READ')")
  public ResponseEntity<ApiResponse<UserResponse>> getProfile(
      @AuthenticationPrincipal UserDetails userDetails) {
    UserResponse response = customerService.getProfile(userDetails.getUsername());
    return ResponseUtil.ok(response);
  }

  /** Update customer profile. PUT /api/customer/profile Requires PROFILE_UPDATE permission. */
  @PutMapping(
      value = "/api/customer/profile",
      consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('PROFILE_UPDATE')")
  public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestPart("data") @Valid UpdateProfileRequest request,
      @RequestPart(value = "ktp", required = false)
          org.springframework.web.multipart.MultipartFile ktp,
      @RequestPart(value = "kk", required = false)
          org.springframework.web.multipart.MultipartFile kk,
      @RequestPart(value = "npwp", required = false)
          org.springframework.web.multipart.MultipartFile npwp) {
    UserProfileResponse response =
        customerService.updateProfile(userDetails.getUsername(), request, ktp, kk, npwp);
    return ResponseUtil.ok("Profile updated successfully", response);
  }

  /**
   * Select a plafond/credit limit. POST /api/customer/plafond Requires PLAFOND_SELECT permission.
   */
  @PostMapping("/api/customer/plafond")
  @PreAuthorize("hasAuthority('PLAFOND_SELECT')")
  public ResponseEntity<ApiResponse<UserPlafondResponse>> selectPlafond(
      @AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestBody SelectPlafondRequest request) {
    UserPlafondResponse response = plafondService.selectPlafond(userDetails.getUsername(), request);
    return ResponseUtil.ok("Plafond selected successfully", response);
  }

  /**
   * Get current customer's active plafond. GET /api/customer/plafond Requires PLAFOND_READ
   * permission.
   */
  @GetMapping("/api/customer/plafond")
  @PreAuthorize("hasAuthority('PLAFOND_READ')")
  public ResponseEntity<ApiResponse<UserPlafondResponse>> getMyPlafond(
      @AuthenticationPrincipal UserDetails userDetails) {
    UserPlafondResponse response = plafondService.getMyPlafond(userDetails.getUsername());
    return ResponseUtil.ok(response);
  }

  /** Get all branches. GET /api/branches Public endpoint - no authentication required. */
  @GetMapping("/api/branches")
  public ResponseEntity<ApiResponse<List<BranchResponse>>> getAllBranches() {
    List<BranchResponse> response = customerService.getAllBranches();
    return ResponseUtil.ok(response);
  }

  /** Register FCM token for push notifications. POST /api/customer/fcm-token */
  @PostMapping("/api/customer/fcm-token")
  @PreAuthorize("hasAuthority('PROFILE_UPDATE')")
  public ResponseEntity<ApiResponse<Void>> registerFcmToken(
      @AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestBody com.gvn.binarbe.dto.request.RegisterFcmTokenRequest request) {
    customerService.registerFcmToken(userDetails.getUsername(), request.getFcmToken());
    return ResponseUtil.ok("FCM token registered successfully", null);
  }
}
