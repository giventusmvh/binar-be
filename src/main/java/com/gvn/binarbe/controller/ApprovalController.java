package com.gvn.binarbe.controller;

import com.gvn.binarbe.dto.request.ApprovalRequest;
import com.gvn.binarbe.dto.response.LoanApplicationResponse;
import com.gvn.binarbe.service.ApprovalService;
import com.gvn.binarbe.util.ApiResponse;
import com.gvn.binarbe.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for loan approval operations.
 * Handles multi-level approval workflow.
 */
@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * Get pending loans for approval.
     * GET /api/approval/pending
     * Returns loans based on user's role and branch.
     * Requires MARKETING, BRANCH_MANAGER, or BACKOFFICE role.
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<LoanApplicationResponse>>> getPendingLoans(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<LoanApplicationResponse> response = approvalService.getPendingLoans(
                userDetails.getUsername());
        return ResponseUtil.ok(response);
    }

    /**
     * Approve a loan application.
     * POST /api/approval/{id}/approve
     * Advances loan to next approval stage.
     * Requires MARKETING, BRANCH_MANAGER, or BACKOFFICE role.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> approve(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ApprovalRequest request) {
        if (request == null) {
            request = new ApprovalRequest();
        }
        LoanApplicationResponse response = approvalService.approve(
                userDetails.getUsername(), id, request);
        return ResponseUtil.ok("Loan approved successfully", response);
    }

    /**
     * Reject a loan application.
     * POST /api/approval/{id}/reject
     * Terminates the loan application.
     * Requires MARKETING, BRANCH_MANAGER, or BACKOFFICE role.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> reject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ApprovalRequest request) {
        LoanApplicationResponse response = approvalService.reject(
                userDetails.getUsername(), id, request);
        return ResponseUtil.ok("Loan rejected", response);
    }

    /**
     * Return a loan application for revision.
     * POST /api/approval/{id}/return
     * Sends loan back to marketing stage.
     * Requires BACKOFFICE role only.
     */
    @PostMapping("/{id}/return")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> returnLoan(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ApprovalRequest request) {
        LoanApplicationResponse response = approvalService.returnLoan(
                userDetails.getUsername(), id, request);
        return ResponseUtil.ok("Loan returned for revision", response);
    }
}
