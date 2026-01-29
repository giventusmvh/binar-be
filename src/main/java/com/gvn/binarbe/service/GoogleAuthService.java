package com.gvn.binarbe.service;

import com.gvn.binarbe.dto.request.GoogleLoginRequest;
import com.gvn.binarbe.dto.response.AuthResponse;

/** Service interface for Google authentication operations. */
public interface GoogleAuthService {

  /**
   * Authenticate user using Google ID token from Android client. If user doesn't exist, creates a
   * new customer account.
   *
   * @param request contains Google ID token and optional FCM token
   * @return authentication response with JWT token
   */
  AuthResponse loginWithGoogle(GoogleLoginRequest request);
}
