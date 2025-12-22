package com.gvn.binarbe.controller;

import com.gvn.binarbe.dto.request.LoanApplicationRequest;
import com.gvn.binarbe.dto.response.LoanApplicationResponse;
import com.gvn.binarbe.dto.response.LoanHistoryResponse;
import com.gvn.binarbe.service.LoanApplicationService;
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
 * Controller for loan application operations.
 * Handles loan submission and tracking for customers.
 */
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    /**
     * Submit a new loan application.
     * POST /api/loans
     * Requires CUSTOMER role.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> submitLoan(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody LoanApplicationRequest request) {
        LoanApplicationResponse response = loanApplicationService.submitLoan(
                userDetails.getUsername(), request);
        return ResponseUtil.created("Loan application submitted successfully", response);
    }

    /**
     * Get all loans for current customer.
     * GET /api/loans
     * Requires CUSTOMER role.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<LoanApplicationResponse>>> getMyLoans(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<LoanApplicationResponse> response = loanApplicationService.getMyLoans(
                userDetails.getUsername());
        return ResponseUtil.ok(response);
    }

    /**
     * Get loan details by ID.
     * GET /api/loans/{id}
     * Requires CUSTOMER role and ownership.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> getLoanById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        LoanApplicationResponse response = loanApplicationService.getLoanById(
                userDetails.getUsername(), id);
        return ResponseUtil.ok(response);
    }

    /**
     * Get approval history for a loan.
     * GET /api/loans/{id}/history
     * Requires CUSTOMER role and ownership.
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<LoanHistoryResponse>>> getLoanHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        List<LoanHistoryResponse> response = loanApplicationService.getLoanHistory(
                userDetails.getUsername(), id);
        return ResponseUtil.ok(response);
    }
}
