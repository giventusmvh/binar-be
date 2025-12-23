package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.dto.request.ApprovalRequest;
import com.gvn.binarbe.dto.response.BranchResponse;
import com.gvn.binarbe.dto.response.LoanApplicationResponse;
import com.gvn.binarbe.dto.response.ProductResponse;
import com.gvn.binarbe.entity.*;
import com.gvn.binarbe.enums.LoanStatus;
import com.gvn.binarbe.enums.RoleName;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.repository.LoanApplicationHistoryRepository;
import com.gvn.binarbe.repository.LoanApplicationRepository;
import com.gvn.binarbe.repository.UserRepository;
import com.gvn.binarbe.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of ApprovalService for multi-level loan approval.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final UserRepository userRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanApplicationHistoryRepository historyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> getPendingLoans(String email) {
        User approver = getApprover(email);
        RoleName role = getHighestRole(approver);
        LoanStatus expectedStatus = getExpectedStatus(role);

        List<LoanApplication> pendingLoans;

        if (role == RoleName.BACKOFFICE) {
            // Backoffice can see all branches
            pendingLoans = loanApplicationRepository.findByStatusWithDetails(expectedStatus);
        } else {
            // Marketing and Branch Manager are branch-restricted
            if (approver.getBranch() == null) {
                throw BusinessException.badRequest("You are not assigned to any branch");
            }
            pendingLoans = loanApplicationRepository.findByStatusAndBranchIdWithDetails(
                    expectedStatus, approver.getBranch().getId());
        }

        return pendingLoans.stream()
                .map(this::mapToLoanResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LoanApplicationResponse approve(String email, Long loanId, ApprovalRequest request) {
        log.info("Approving loan {} by {}", loanId, email);

        User approver = getApprover(email);
        RoleName role = getHighestRole(approver);
        LoanApplication loan = getLoanForApproval(loanId, approver, role);

        // Determine new status based on role
        LoanStatus newStatus = switch (role) {
            case MARKETING -> LoanStatus.MARKETING_APPROVED;
            case BRANCH_MANAGER -> LoanStatus.BRANCH_MANAGER_APPROVED;
            case BACKOFFICE -> LoanStatus.APPROVED;
            default -> throw BusinessException.forbidden("You don't have approval permission");
        };

        // Update loan status
        loan.setStatus(newStatus);
        loan = loanApplicationRepository.save(loan);

        // Create history entry
        createHistoryEntry(loan, approver, role, newStatus, request.getNote());

        log.info("Loan {} approved with status {}", loanId, newStatus);

        return mapToLoanResponse(loan);
    }

    @Override
    @Transactional
    public LoanApplicationResponse reject(String email, Long loanId, ApprovalRequest request) {
        log.info("Rejecting loan {} by {}", loanId, email);

        User approver = getApprover(email);
        RoleName role = getHighestRole(approver);
        LoanApplication loan = getLoanForApproval(loanId, approver, role);

        // Determine rejection status based on role
        LoanStatus newStatus = switch (role) {
            case MARKETING -> LoanStatus.MARKETING_REJECTED;
            case BRANCH_MANAGER -> LoanStatus.BRANCH_MANAGER_REJECTED;
            case BACKOFFICE -> LoanStatus.REJECTED;
            default -> throw BusinessException.forbidden("You don't have rejection permission");
        };

        // Update loan status
        loan.setStatus(newStatus);
        loan = loanApplicationRepository.save(loan);

        // Create history entry
        String note = request.getNote() != null ? request.getNote() : "Loan application rejected";
        createHistoryEntry(loan, approver, role, newStatus, note);

        log.info("Loan {} rejected with status {}", loanId, newStatus);

        return mapToLoanResponse(loan);
    }

    @Override
    @Transactional
    public LoanApplicationResponse returnLoan(String email, Long loanId, ApprovalRequest request) {
        log.info("Returning loan {} by {}", loanId, email);

        User approver = getApprover(email);
        RoleName role = getHighestRole(approver);

        // Only backoffice can return
        if (role != RoleName.BACKOFFICE) {
            throw BusinessException.forbidden("Only Backoffice can return loan applications");
        }

        LoanApplication loan = getLoanForApproval(loanId, approver, role);

        // Update loan status to RETURNED (goes back to marketing)
        loan.setStatus(LoanStatus.RETURNED);
        loan = loanApplicationRepository.save(loan);

        // Create history entry
        String note = request.getNote() != null ? request.getNote() : "Loan application returned for revision";
        createHistoryEntry(loan, approver, role, LoanStatus.RETURNED, note);

        log.info("Loan {} returned for revision", loanId);

        return mapToLoanResponse(loan);
    }

    private User getApprover(String email) {
        return userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> BusinessException.notFound("User not found"));
    }

    private RoleName getHighestRole(User user) {
        Set<RoleName> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        // Priority: BACKOFFICE > BRANCH_MANAGER > MARKETING
        if (roleNames.contains(RoleName.BACKOFFICE))
            return RoleName.BACKOFFICE;
        if (roleNames.contains(RoleName.BRANCH_MANAGER))
            return RoleName.BRANCH_MANAGER;
        if (roleNames.contains(RoleName.MARKETING))
            return RoleName.MARKETING;

        throw BusinessException.forbidden("You don't have any approval role");
    }

    private LoanStatus getExpectedStatus(RoleName role) {
        return switch (role) {
            case MARKETING -> LoanStatus.SUBMITTED;
            case BRANCH_MANAGER -> LoanStatus.MARKETING_APPROVED;
            case BACKOFFICE -> LoanStatus.BRANCH_MANAGER_APPROVED;
            default -> throw BusinessException.forbidden("Invalid role for approval");
        };
    }

    private LoanApplication getLoanForApproval(Long loanId, User approver, RoleName role) {
        LoanApplication loan = loanApplicationRepository.findByIdWithDetails(loanId)
                .orElseThrow(() -> BusinessException.notFound("Loan application not found"));

        // Check expected status
        LoanStatus expectedStatus = getExpectedStatus(role);

        // For returned loans, marketing should be able to process them
        if (role == RoleName.MARKETING && loan.getStatus() == LoanStatus.RETURNED) {
            // Allow marketing to process returned loans
        } else if (loan.getStatus() != expectedStatus) {
            throw BusinessException.badRequest("Loan is not in the correct status for your approval. " +
                    "Current status: " + loan.getStatus() + ", Expected: " + expectedStatus);
        }

        // Check branch restriction for Marketing and Branch Manager
        if (role != RoleName.BACKOFFICE) {
            if (approver.getBranch() == null) {
                throw BusinessException.badRequest("You are not assigned to any branch");
            }
            if (!loan.getBranch().getId().equals(approver.getBranch().getId())) {
                throw BusinessException.forbidden("You can only process loans from your branch");
            }
        }

        return loan;
    }

    private void createHistoryEntry(LoanApplication loan, User approver, RoleName role,
            LoanStatus status, String note) {
        LoanApplicationHistory history = LoanApplicationHistory.builder()
                .loanApplication(loan)
                .approvedBy(approver)
                .approvedByRole(role.name())
                .approvedByBranchId(approver.getBranch() != null ? approver.getBranch().getId().intValue() : null)
                .status(status)
                .note(note)
                .build();

        historyRepository.save(history);
    }

    private LoanApplicationResponse mapToLoanResponse(LoanApplication loan) {
        return LoanApplicationResponse.builder()
                .id(loan.getId())
                .customerName(loan.getCustomer().getName())
                .customerEmail(loan.getCustomer().getEmail())
                .product(mapToProductResponse(loan.getProduct()))
                .branch(mapToBranchResponse(loan.getBranch()))
                .requestedAmount(loan.getRequestedAmount())
                .requestedTenor(loan.getRequestedTenor())
                .requestedRate(loan.getRequestedRate())
                .status(loan.getStatus())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
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
