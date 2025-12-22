package com.gvn.binarbe.service;

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
}
