package com.gvn.binarbe.controller;

import com.gvn.binarbe.dto.request.ForgotPasswordRequest;
import com.gvn.binarbe.dto.request.GoogleLoginRequest;
import com.gvn.binarbe.dto.request.LoginRequest;
import com.gvn.binarbe.dto.request.RegisterRequest;
import com.gvn.binarbe.dto.request.ResetPasswordRequest;
import com.gvn.binarbe.dto.response.AuthResponse;
import com.gvn.binarbe.service.AuthService;
import com.gvn.binarbe.service.GoogleAuthService;
import com.gvn.binarbe.util.ApiResponse;
import com.gvn.binarbe.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication operations. Handles customer registration, user login, password
 * reset, and logout.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final GoogleAuthService googleAuthService;

  /** Register a new customer. POST /api/auth/register */
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<AuthResponse>> register(
      @Valid @RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);
    return ResponseUtil.created("Registration successful", response);
  }

  /** Authenticate user and get JWT token. POST /api/auth/login */
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseUtil.ok("Login successful", response);
  }

  /** Request password reset email. POST /api/auth/forgot-password */
  @PostMapping("/forgot-password")
  public ResponseEntity<ApiResponse<Void>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {
    authService.forgotPassword(request);
    return ResponseUtil.ok("If the email exists, a password reset link has been sent");
  }

  /** Reset password using token from email. POST /api/auth/reset-password */
  @PostMapping("/reset-password")
  public ResponseEntity<ApiResponse<Void>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ResponseUtil.ok("Password reset successfully");
  }

  /** Logout user by blacklisting current token. POST /api/auth/logout */
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      authService.logout(token);
    }
    return ResponseUtil.ok("Logged out successfully");
  }

  /** Authenticate user using Google ID token from Android. POST /api/auth/google-login */
  @PostMapping("/google-login")
  public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
      @Valid @RequestBody GoogleLoginRequest request) {
    AuthResponse response = googleAuthService.loginWithGoogle(request);
    return ResponseUtil.ok("Google login successful", response);
  }
}
