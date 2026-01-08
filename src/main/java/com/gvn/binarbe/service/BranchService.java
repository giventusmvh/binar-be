package com.gvn.binarbe.service;

import com.gvn.binarbe.dto.request.CreateBranchRequest;
import com.gvn.binarbe.dto.request.UpdateBranchRequest;
import com.gvn.binarbe.dto.response.BranchResponse;
import java.util.List;

/** Service interface for branch operations. */
public interface BranchService {

  /**
   * Get all branches.
   *
   * @return list of branches
   */
  List<BranchResponse> getAllBranches();

  /**
   * Get branch by ID.
   *
   * @param id branch ID
   * @return branch details
   */
  BranchResponse getBranchById(Long id);

  /**
   * Create a new branch.
   *
   * @param request branch creation data
   * @return created branch details
   */
  BranchResponse createBranch(CreateBranchRequest request);

  /**
   * Update an existing branch.
   *
   * @param id branch ID
   * @param request branch update data
   * @return updated branch details
   */
  BranchResponse updateBranch(Long id, UpdateBranchRequest request);

  /**
   * Delete a branch.
   *
   * @param id branch ID
   */
  void deleteBranch(Long id);
}
