package com.gvn.binarbe.service;

import com.gvn.binarbe.dto.request.UpdateProfileRequest;
import com.gvn.binarbe.dto.response.BranchResponse;
import com.gvn.binarbe.dto.response.ProductResponse;
import com.gvn.binarbe.dto.response.UserProfileResponse;
import com.gvn.binarbe.dto.response.UserResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/** Service interface for customer operations. */
public interface CustomerService {

  /**
   * Get current user profile.
   *
   * @param email user email
   * @return user response with profile
   */
  UserResponse getProfile(String email);

  /**
   * Update customer profile.
   *
   * @param email user email
   * @param request profile update data
   * @return updated profile response
   */
  UserProfileResponse updateProfile(
      String email,
      UpdateProfileRequest request,
      MultipartFile ktp,
      MultipartFile kk,
      MultipartFile npwp);

  /**
   * Check if customer profile is complete.
   *
   * @param email user email
   * @return true if profile is complete
   */
  boolean isProfileComplete(String email);

  /**
   * Get all available products.
   *
   * @return list of products
   */
  List<ProductResponse> getAllProducts();

  /**
   * Get all branches.
   *
   * @return list of branches
   */
  List<BranchResponse> getAllBranches();
}
