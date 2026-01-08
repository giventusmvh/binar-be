package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.dto.request.LoanApplicationRequest;
import com.gvn.binarbe.dto.response.BranchResponse;
import com.gvn.binarbe.dto.response.LoanApplicationResponse;
import com.gvn.binarbe.dto.response.LoanHistoryResponse;
import com.gvn.binarbe.dto.response.ProductResponse;
import com.gvn.binarbe.entity.*;
import com.gvn.binarbe.enums.LoanStatus;
import com.gvn.binarbe.enums.RoleName;
import com.gvn.binarbe.enums.UserType;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.repository.*;
import com.gvn.binarbe.service.LoanApplicationService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of LoanApplicationService for loan operations. */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanApplicationServiceImpl implements LoanApplicationService {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final BranchRepository branchRepository;
  private final LoanApplicationRepository loanApplicationRepository;
  private final LoanApplicationHistoryRepository historyRepository;
  private final UserPlafondRepository userPlafondRepository;

  @Override
  @Transactional
  public LoanApplicationResponse submitLoan(String email, LoanApplicationRequest request) {
    log.info("Submitting loan for customer: {}", email);

    User customer =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    // Check if user has any pending loan applications
    List<LoanStatus> pendingStatuses =
        List.of(
            LoanStatus.SUBMITTED,
            LoanStatus.MARKETING_APPROVED,
            LoanStatus.BRANCH_MANAGER_APPROVED);

    boolean hasPendingLoan =
        loanApplicationRepository.existsByCustomerIdAndStatusIn(customer.getId(), pendingStatuses);

    if (hasPendingLoan) {
      throw BusinessException.badRequest(
          "You already have a pending loan application. "
              + "Please wait until it is fully approved or rejected before submitting a new one.");
    }

    // Check if profile is complete
    UserProfile profile =
        userProfileRepository
            .findByUserId(customer.getId())
            .orElseThrow(
                () ->
                    BusinessException.badRequest(
                        "Profile not found. Please complete your profile first."));

    if (!profile.isComplete()) {
      throw BusinessException.badRequest(
          "Please complete your profile before submitting a loan application. "
              + "Required fields: NIK, birthdate, phone, and address.");
    }

    // Get user's active plafond
    UserPlafond userPlafond =
        userPlafondRepository
            .findByUserIdWithProduct(customer.getId())
            .orElseThrow(
                () ->
                    BusinessException.badRequest(
                        "Please select a plafond first before submitting a loan application."));

    Product product = userPlafond.getProduct();

    // Validate requested amount against remaining plafond
    if (request.getAmount().compareTo(userPlafond.getRemainingAmount()) > 0) {
      throw BusinessException.badRequest(
          "Requested amount exceeds remaining plafond. Remaining: Rp "
              + userPlafond.getRemainingAmount());
    }

    // Validate requested tenor against plafond limit
    if (request.getTenor() > product.getTenor()) {
      throw BusinessException.badRequest(
          "Requested tenor exceeds plafond limit. Maximum: " + product.getTenor() + " months");
    }

    // Validate requested interest rate (must be >= plafond rate)
    if (request.getInterestRate().compareTo(product.getInterestRate()) < 0) {
      throw BusinessException.badRequest(
          "Interest rate cannot be lower than plafond minimum rate. Minimum: "
              + product.getInterestRate()
              + "%");
    }

    // Get branch
    Branch branch =
        branchRepository
            .findById(request.getBranchId())
            .orElseThrow(() -> BusinessException.notFound("Branch not found"));

    // Create loan application with requested values and customer snapshot
    LoanApplication loanApplication =
        LoanApplication.builder()
            .customer(customer)
            .product(product)
            .branch(branch)
            .requestedAmount(request.getAmount())
            .requestedTenor(request.getTenor())
            .requestedRate(request.getInterestRate())
            // Snapshot customer data at submission time
            .customerNameSnapshot(customer.getName())
            .customerEmailSnapshot(customer.getEmail())
            .customerNikSnapshot(profile.getNik())
            .customerPhoneSnapshot(profile.getPhone())
            .customerAddressSnapshot(profile.getAddress())
            .customerBirthdateSnapshot(profile.getBirthdate())
            .status(LoanStatus.SUBMITTED)
            .build();

    loanApplication = loanApplicationRepository.save(loanApplication);

    // Create initial history entry
    LoanApplicationHistory history =
        LoanApplicationHistory.builder()
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
    User customer =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    return loanApplicationRepository.findByCustomerIdWithDetails(customer.getId()).stream()
        .map(this::mapToLoanResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public LoanApplicationResponse getLoanById(String email, Long loanId) {
    User user =
        userRepository
            .findByEmailWithRoles(email)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    LoanApplication loan =
        loanApplicationRepository
            .findByIdWithDetails(loanId)
            .orElseThrow(() -> BusinessException.notFound("Loan application not found"));

    validateLoanAccess(user, loan);

    return mapToLoanResponse(loan);
  }

  @Override
  @Transactional(readOnly = true)
  public List<LoanHistoryResponse> getLoanHistory(String email, Long loanId) {
    User user =
        userRepository
            .findByEmailWithRoles(email)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    LoanApplication loan =
        loanApplicationRepository
            .findById(loanId)
            .orElseThrow(() -> BusinessException.notFound("Loan application not found"));

    validateLoanAccess(user, loan);

    return historyRepository.findByLoanApplicationIdWithApprover(loanId).stream()
        .map(this::mapToHistoryResponse)
        .collect(Collectors.toList());
  }

  private void validateLoanAccess(User user, LoanApplication loan) {
    // 1. Owner always has access
    if (loan.getCustomer().getId().equals(user.getId())) {
      return;
    }

    // 2. Check internal roles
    if (user.getUserType() == UserType.INTERNAL) {
      // Superadmin and Backoffice can see all
      boolean canSeeAll =
          user.getRoles().stream()
              .anyMatch(
                  r -> r.getName() == RoleName.SUPERADMIN || r.getName() == RoleName.BACKOFFICE);

      if (canSeeAll) return;

      // Marketing and Branch Manager can see same branch
      boolean sameBranch =
          user.getBranch() != null
              && loan.getBranch() != null
              && user.getBranch().getId().equals(loan.getBranch().getId());

      if (sameBranch) return;
    }

    throw BusinessException.forbidden("You don't have access to this loan application");
  }

  private LoanApplicationResponse mapToLoanResponse(LoanApplication loan) {
    return LoanApplicationResponse.builder()
        .id(loan.getId())
        // Use snapshot data (preserved from submission time)
        .customerName(loan.getCustomerNameSnapshot())
        .customerEmail(loan.getCustomerEmailSnapshot())
        .customerNik(loan.getCustomerNikSnapshot())
        .customerPhone(loan.getCustomerPhoneSnapshot())
        .customerAddress(loan.getCustomerAddressSnapshot())
        .customerBirthdate(loan.getCustomerBirthdateSnapshot())
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

  private LoanHistoryResponse mapToHistoryResponse(LoanApplicationHistory history) {
    return LoanHistoryResponse.builder()
        .id(history.getId())
        .approvedByName(history.getApprovedBy().getName())
        .approvedByRole(history.getApprovedByRole())
        .approvedByBranch(
            history.getApprovedByBranchId() != null
                ? "Branch ID: " + history.getApprovedByBranchId()
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
