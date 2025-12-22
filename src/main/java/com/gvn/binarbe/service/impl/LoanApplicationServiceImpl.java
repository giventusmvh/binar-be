package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.dto.request.LoanApplicationRequest;
import com.gvn.binarbe.dto.response.BranchResponse;
import com.gvn.binarbe.dto.response.LoanApplicationResponse;
import com.gvn.binarbe.dto.response.LoanHistoryResponse;
import com.gvn.binarbe.dto.response.ProductResponse;
import com.gvn.binarbe.entity.*;
import com.gvn.binarbe.enums.LoanStatus;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.repository.*;
import com.gvn.binarbe.service.LoanApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of LoanApplicationService for loan operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanApplicationHistoryRepository historyRepository;

    @Override
    @Transactional
    public LoanApplicationResponse submitLoan(String email, LoanApplicationRequest request) {
        log.info("Submitting loan for customer: {}", email);

        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        // Check if profile is complete
        UserProfile profile = userProfileRepository.findByUserId(customer.getId())
                .orElseThrow(
                        () -> BusinessException.badRequest("Profile not found. Please complete your profile first."));

        if (!profile.isComplete()) {
            throw BusinessException.badRequest("Please complete your profile before submitting a loan application. " +
                    "Required fields: NIK, birthdate, phone, and address.");
        }

        // Get product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> BusinessException.notFound("Product not found"));

        // Get branch
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> BusinessException.notFound("Branch not found"));

        // Create loan application
        LoanApplication loanApplication = LoanApplication.builder()
                .customer(customer)
                .product(product)
                .branch(branch)
                .status(LoanStatus.SUBMITTED)
                .build();

        loanApplication = loanApplicationRepository.save(loanApplication);

        // Create initial history entry
        LoanApplicationHistory history = LoanApplicationHistory.builder()
                .loanApplication(loanApplication)
                .approvedBy(customer)
                .approvedByRole("CUSTOMER")
                .status(LoanStatus.SUBMITTED)
                .note("Loan application submitted")
                .build();

        historyRepository.save(history);

        log.info("Loan application submitted: ID={}", loanApplication.getId());

        return mapToLoanResponse(loanApplication);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> getMyLoans(String email) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        return loanApplicationRepository.findByCustomerIdWithDetails(customer.getId()).stream()
                .map(this::mapToLoanResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LoanApplicationResponse getLoanById(String email, Long loanId) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        LoanApplication loan = loanApplicationRepository.findByIdWithDetails(loanId)
                .orElseThrow(() -> BusinessException.notFound("Loan application not found"));

        // Verify ownership
        if (!loan.getCustomer().getId().equals(customer.getId())) {
            throw BusinessException.forbidden("You don't have access to this loan application");
        }

        return mapToLoanResponse(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanHistoryResponse> getLoanHistory(String email, Long loanId) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        LoanApplication loan = loanApplicationRepository.findById(loanId)
                .orElseThrow(() -> BusinessException.notFound("Loan application not found"));

        // Verify ownership
        if (!loan.getCustomer().getId().equals(customer.getId())) {
            throw BusinessException.forbidden("You don't have access to this loan application");
        }

        return historyRepository.findByLoanApplicationIdWithApprover(loanId).stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    private LoanApplicationResponse mapToLoanResponse(LoanApplication loan) {
        return LoanApplicationResponse.builder()
                .id(loan.getId())
                .customerName(loan.getCustomer().getName())
                .customerEmail(loan.getCustomer().getEmail())
                .product(mapToProductResponse(loan.getProduct()))
                .branch(mapToBranchResponse(loan.getBranch()))
                .status(loan.getStatus())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .build();
    }

    private LoanHistoryResponse mapToHistoryResponse(LoanApplicationHistory history) {
        return LoanHistoryResponse.builder()
                .id(history.getId())
                .approvedByName(history.getApprovedBy().getName())
                .approvedByRole(history.getApprovedByRole())
                .approvedByBranch(
                        history.getApprovedByBranchId() != null ? "Branch ID: " + history.getApprovedByBranchId()
                                : "N/A")
                .status(history.getStatus())
                .note(history.getNote())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .amount(product.getAmount())
                .tenor(product.getTenor())
                .interestRate(product.getInterestRate())
                .build();
    }

    private BranchResponse mapToBranchResponse(Branch branch) {
        return BranchResponse.builder()
                .id(branch.getId())
                .code(branch.getCode())
                .location(branch.getLocation())
                .build();
    }
}
