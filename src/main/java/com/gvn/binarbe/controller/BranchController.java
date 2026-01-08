package com.gvn.binarbe.controller;

import com.gvn.binarbe.dto.request.CreateBranchRequest;
import com.gvn.binarbe.dto.request.UpdateBranchRequest;
import com.gvn.binarbe.dto.response.BranchResponse;
import com.gvn.binarbe.service.BranchService;
import com.gvn.binarbe.util.ApiResponse;
import com.gvn.binarbe.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Controller for branch management operations. */
@RestController
@RequestMapping("/api/admin/branches")
@RequiredArgsConstructor
public class BranchController {

  private final BranchService branchService;

  /** Create a new branch. POST /api/admin/branches Requires SUPERADMIN role. */
  @PostMapping
  public ResponseEntity<ApiResponse<BranchResponse>> createBranch(
      @Valid @RequestBody CreateBranchRequest request) {
    BranchResponse response = branchService.createBranch(request);
    return ResponseUtil.created("Branch created successfully", response);
  }

  /** Update an existing branch. PUT /api/admin/branches/{id} Requires SUPERADMIN role. */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<BranchResponse>> updateBranch(
      @PathVariable Long id, @Valid @RequestBody UpdateBranchRequest request) {
    BranchResponse response = branchService.updateBranch(id, request);
    return ResponseUtil.ok("Branch updated successfully", response);
  }

  /** Delete a branch. DELETE /api/admin/branches/{id} Requires SUPERADMIN role. */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable Long id) {
    branchService.deleteBranch(id);
    return ResponseUtil.ok("Branch deleted successfully", null);
  }
}
