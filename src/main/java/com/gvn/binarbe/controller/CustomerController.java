package com.gvn.binarbe.controller;

import com.gvn.binarbe.dto.request.SelectPlafondRequest;
import com.gvn.binarbe.dto.request.UpdateProfileRequest;
import com.gvn.binarbe.dto.response.BranchResponse;
import com.gvn.binarbe.dto.response.ProductResponse;
import com.gvn.binarbe.dto.response.UserPlafondResponse;
import com.gvn.binarbe.dto.response.UserProfileResponse;
import com.gvn.binarbe.dto.response.UserResponse;
import com.gvn.binarbe.service.CustomerService;
import com.gvn.binarbe.service.PlafondService;
import com.gvn.binarbe.util.ApiResponse;
import com.gvn.binarbe.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for customer operations.
 * Handles profile management, plafond selection, and browsing
 * products/branches.
 */
@RestController
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final PlafondService plafondService;

    /**
     * Get current customer profile.
     * GET /api/customer/profile
     * Requires CUSTOMER role.
     */
    @GetMapping("/api/customer/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse response = customerService.getProfile(userDetails.getUsername());
        return ResponseUtil.ok(response);
    }

    /**
     * Update customer profile.
     * PUT /api/customer/profile
     * Requires CUSTOMER role.
     */
    @PutMapping("/api/customer/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse response = customerService.updateProfile(userDetails.getUsername(), request);
        return ResponseUtil.ok("Profile updated successfully", response);
    }

    /**
     * Select a plafond/credit limit.
     * POST /api/customer/plafond
     * Requires CUSTOMER role.
     */
    @PostMapping("/api/customer/plafond")
    public ResponseEntity<ApiResponse<UserPlafondResponse>> selectPlafond(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SelectPlafondRequest request) {
        UserPlafondResponse response = plafondService.selectPlafond(userDetails.getUsername(), request);
        return ResponseUtil.ok("Plafond selected successfully", response);
    }

    /**
     * Get current customer's active plafond.
     * GET /api/customer/plafond
     * Requires CUSTOMER role.
     */
    @GetMapping("/api/customer/plafond")
    public ResponseEntity<ApiResponse<UserPlafondResponse>> getMyPlafond(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserPlafondResponse response = plafondService.getMyPlafond(userDetails.getUsername());
        return ResponseUtil.ok(response);
    }

    /**
     * Get all available products.
     * GET /api/products
     * Public endpoint - no authentication required.
     */
    @GetMapping("/api/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<ProductResponse> response = customerService.getAllProducts();
        return ResponseUtil.ok(response);
    }

    /**
     * Get all branches.
     * GET /api/branches
     * Public endpoint - no authentication required.
     */
    @GetMapping("/api/branches")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getAllBranches() {
        List<BranchResponse> response = customerService.getAllBranches();
        return ResponseUtil.ok(response);
    }
}
