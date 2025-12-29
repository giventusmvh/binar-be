package com.gvn.binarbe.service;

import com.gvn.binarbe.dto.request.ForgotPasswordRequest;
import com.gvn.binarbe.dto.request.LoginRequest;
import com.gvn.binarbe.dto.request.RegisterRequest;
import com.gvn.binarbe.dto.request.ResetPasswordRequest;
import com.gvn.binarbe.dto.response.AuthResponse;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {

    /**
     * Register a new customer.
     *
     * @param request registration data
     * @return authentication response with JWT token
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticate user and generate JWT token.
     *
     * @param request login credentials
     * @return authentication response with JWT token
     */
    AuthResponse login(LoginRequest request);

    /**
     * Initiate password reset by sending email with reset token.
     *
     * @param request contains user email
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * Reset password using token received via email.
     * Also invalidates all existing tokens for the user.
     *
     * @param request contains token and new password
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * Logout user by blacklisting their current token.
     *
     * @param token JWT token to blacklist
     */
    void logout(String token);
}
