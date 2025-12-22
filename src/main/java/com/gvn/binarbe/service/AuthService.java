package com.gvn.binarbe.service;

import com.gvn.binarbe.dto.request.ChangePasswordRequest;
import com.gvn.binarbe.dto.request.LoginRequest;
import com.gvn.binarbe.dto.request.RegisterRequest;
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
     * Change user password.
     * Requires current password verification before allowing password change.
     *
     * @param userId  the ID of the user changing their password
     * @param request change password data containing current and new password
     */
    void changePassword(Long userId, ChangePasswordRequest request);
}
