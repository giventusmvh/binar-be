package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.dto.request.LoanApplicationRequest;
import com.gvn.binarbe.dto.response.LoanApplicationResponse;
import com.gvn.binarbe.dto.response.LoanHistoryResponse;
import com.gvn.binarbe.entity.*;
import com.gvn.binarbe.enums.LoanStatus;
import com.gvn.binarbe.enums.RoleName;
import com.gvn.binarbe.enums.UserType;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.mapper.LoanApplicationMapper;
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
  private final LoanApplicationMapper loanApplicationMapper;

  @Override
  @Transactional
  public LoanApplicationResponse submitLoan(String email, LoanApplicationRequest request) {
    log.info("Submitting loan for customer: {}", email);

    User customer =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

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

    UserPlafond userPlafond =
        userPlafondRepository
            .findByUserIdWithProduct(customer.getId())
            .orElseThrow(
                () ->
                    BusinessException.badRequest(
                        "Please select a plafond first before submitting a loan application."));

    Product product = userPlafond.getProduct();

    if (request.getAmount().compareTo(userPlafond.getRemainingAmount()) > 0) {
      throw BusinessException.badRequest(
          "Requested amount exceeds remaining plafond. Remaining: Rp "
              + userPlafond.getRemainingAmount());
    }

    if (request.getTenor() > product.getTenor()) {
      throw BusinessException.badRequest(
          "Requested tenor exceeds plafond limit. Maximum: " + product.getTenor() + " months");
    }

    if (request.getInterestRate().compareTo(product.getInterestRate()) < 0) {
      throw BusinessException.badRequest(
          "Interest rate cannot be lower than plafond minimum rate. Minimum: "
              + product.getInterestRate()
              + "%");
    }

    Branch branch =
        branchRepository
            .findById(request.getBranchId())
            .orElseThrow(() -> BusinessException.notFound("Branch not found"));

    LoanApplication loanApplication =
        LoanApplication.builder()
            .customer(customer)
            .product(product)
            .branch(branch)
            .requestedAmount(request.getAmount())
            .requestedTenor(request.getTenor())
            .requestedRate(request.getInterestRate())
            .customerNameSnapshot(customer.getName())
            .customerEmailSnapshot(customer.getEmail())
            .customerNikSnapshot(profile.getNik())
            .customerPhoneSnapshot(profile.getPhone())
            .customerAddressSnapshot(profile.getAddress())
            .customerBirthdateSnapshot(profile.getBirthdate())
            .customerKtpPathSnapshot(profile.getKtpPath())
            .customerKkPathSnapshot(profile.getKkPath())
            .customerNpwpPathSnapshot(profile.getNpwpPath())
            .customerBankNameSnapshot(profile.getBankName())
            .customerAccountNumberSnapshot(profile.getAccountNumber())
            .customerAccountHolderNameSnapshot(profile.getAccountHolderName())
            .status(LoanStatus.SUBMITTED)
            .build();

    loanApplication = loanApplicationRepository.save(loanApplication);

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

    return loanApplicationMapper.toResponse(loanApplication);
  }

  @Override
  @Transactional(readOnly = true)
  public List<LoanApplicationResponse> getMyLoans(String email) {
    User customer =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    return loanApplicationRepository.findByCustomerIdWithDetails(customer.getId()).stream()
        .map(loanApplicationMapper::toResponse)
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

    return loanApplicationMapper.toResponse(loan);
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
        .map(loanApplicationMapper::toHistoryResponse)
        .collect(Collectors.toList());
  }

  private void validateLoanAccess(User user, LoanApplication loan) {
    if (loan.getCustomer().getId().equals(user.getId())) {
      return;
    }

    if (user.getUserType() == UserType.INTERNAL) {
      boolean canSeeAll =
          user.getRoles().stream()
              .anyMatch(
                  r -> r.getName() == RoleName.SUPERADMIN || r.getName() == RoleName.BACKOFFICE);

      if (canSeeAll) return;

      boolean sameBranch =
          user.getBranch() != null
              && loan.getBranch() != null
              && user.getBranch().getId().equals(loan.getBranch().getId());

      if (sameBranch) return;
    }

    throw BusinessException.forbidden("You don't have access to this loan application");
  }
}
