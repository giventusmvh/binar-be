package com.gvn.binarbe.controller;

import com.gvn.binarbe.dto.request.ChangePasswordRequest;
import com.gvn.binarbe.dto.request.LoginRequest;
import com.gvn.binarbe.dto.request.RegisterRequest;
import com.gvn.binarbe.dto.response.AuthResponse;
import com.gvn.binarbe.entity.User;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.repository.UserRepository;
import com.gvn.binarbe.service.AuthService;
import com.gvn.binarbe.util.ApiResponse;
import com.gvn.binarbe.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication operations.
 * Handles customer registration, user login, and password management.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    /**
     * Register a new customer.
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseUtil.created("Registration successful", response);
    }

    /**
     * Authenticate user and get JWT token.
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseUtil.ok("Login successful", response);
    }

    /**
     * Change user password.
     * Requires authentication and current password verification.
     * POST /api/auth/change-password
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        // Get user ID from authenticated user
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        authService.changePassword(user.getId(), request);
        return ResponseUtil.ok("Password changed successfully");
    }
}
