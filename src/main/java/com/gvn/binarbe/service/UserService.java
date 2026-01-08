package com.gvn.binarbe.service;

import com.gvn.binarbe.dto.response.UserResponse;

/** Service interface for common user operations accessible by multiple roles. */
public interface UserService {

  /**
   * Get user by ID.
   *
   * @param userId user ID
   * @return user details
   */
  UserResponse getUserById(Long userId);
}
