package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.dto.request.ApprovalRequest;
import com.gvn.binarbe.dto.response.LoanApplicationResponse;
import com.gvn.binarbe.dto.response.MyApprovalHistoryResponse;
import com.gvn.binarbe.entity.*;
import com.gvn.binarbe.enums.LoanStatus;
import com.gvn.binarbe.enums.RoleName;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.mapper.LoanApplicationMapper;
import com.gvn.binarbe.repository.LoanApplicationHistoryRepository;
import com.gvn.binarbe.repository.LoanApplicationRepository;
import com.gvn.binarbe.repository.UserPlafondRepository;
import com.gvn.binarbe.repository.UserRepository;
import com.gvn.binarbe.service.ApprovalService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of ApprovalService for multi-level loan approval. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

  private final UserRepository userRepository;
  private final LoanApplicationRepository loanApplicationRepository;
  private final LoanApplicationHistoryRepository historyRepository;
  private final UserPlafondRepository userPlafondRepository;
  private final LoanApplicationMapper loanApplicationMapper;

  @Override
  @Transactional(readOnly = true)
  public List<LoanApplicationResponse> getPendingLoans(String email) {
    User approver = getApprover(email);
    RoleName role = getHighestRole(approver);
    LoanStatus expectedStatus = getExpectedStatus(role);

    List<LoanApplication> pendingLoans;

    if (role == RoleName.BACKOFFICE) {
      pendingLoans = loanApplicationRepository.findByStatusWithDetails(expectedStatus);
    } else {
      if (approver.getBranch() == null) {
        throw BusinessException.badRequest("You are not assigned to any branch");
      }
      pendingLoans =
          loanApplicationRepository.findByStatusAndBranchIdWithDetails(
              expectedStatus, approver.getBranch().getId());
    }

    return pendingLoans.stream()
        .map(loanApplicationMapper::toResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public LoanApplicationResponse approve(String email, Long loanId, ApprovalRequest request) {
    log.info("Approving loan {} by {}", loanId, email);

    User approver = getApprover(email);
    RoleName role = getHighestRole(approver);
    LoanApplication loan = getLoanForApproval(loanId, approver, role);

    LoanStatus newStatus =
        switch (role) {
          case MARKETING -> LoanStatus.MARKETING_APPROVED;
          case BRANCH_MANAGER -> LoanStatus.BRANCH_MANAGER_APPROVED;
          case BACKOFFICE -> LoanStatus.DISBURSED;
          default -> throw BusinessException.forbidden("You don't have approval permission");
        };

    loan.setStatus(newStatus);
    loan = loanApplicationRepository.save(loan);

    if (newStatus == LoanStatus.DISBURSED) {
      UserPlafond userPlafond =
          userPlafondRepository
              .findByUserIdWithProduct(loan.getCustomer().getId())
              .orElseThrow(() -> BusinessException.notFound("Plafond not found"));

      java.math.BigDecimal newRemaining =
          userPlafond.getRemainingAmount().subtract(loan.getRequestedAmount());
      userPlafond.setRemainingAmount(newRemaining);

      if (newRemaining.compareTo(java.math.BigDecimal.ZERO) <= 0) {
        userPlafond.setIsActive(false);
        log.info(
            "Plafond for customer {} is now inactive (remaining amount depleted)",
            loan.getCustomer().getEmail());
      }
      userPlafondRepository.save(userPlafond);
    }

    createHistoryEntry(loan, approver, role, newStatus, request.getNote());

    log.info("Loan {} approved with status {}", loanId, newStatus);

    return loanApplicationMapper.toResponse(loan);
  }

  @Override
  @Transactional
  public LoanApplicationResponse reject(String email, Long loanId, ApprovalRequest request) {
    log.info("Rejecting loan {} by {}", loanId, email);

    User approver = getApprover(email);
    RoleName role = getHighestRole(approver);
    LoanApplication loan = getLoanForApproval(loanId, approver, role);

    LoanStatus newStatus =
        switch (role) {
          case MARKETING -> LoanStatus.MARKETING_REJECTED;
          case BRANCH_MANAGER -> LoanStatus.BRANCH_MANAGER_REJECTED;
          case BACKOFFICE -> LoanStatus.REJECTED;
          default -> throw BusinessException.forbidden("You don't have rejection permission");
        };

    loan.setStatus(newStatus);
    loan = loanApplicationRepository.save(loan);

    String note = request.getNote() != null ? request.getNote() : "Loan application rejected";
    createHistoryEntry(loan, approver, role, newStatus, note);

    log.info("Loan {} rejected with status {}", loanId, newStatus);

    return loanApplicationMapper.toResponse(loan);
  }

  private User getApprover(String email) {
    return userRepository
        .findByEmailWithRoles(email)
        .orElseThrow(() -> BusinessException.notFound("User not found"));
  }

  private RoleName getHighestRole(User user) {
    Set<RoleName> roleNames =
        user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

    if (roleNames.contains(RoleName.BACKOFFICE)) return RoleName.BACKOFFICE;
    if (roleNames.contains(RoleName.BRANCH_MANAGER)) return RoleName.BRANCH_MANAGER;
    if (roleNames.contains(RoleName.MARKETING)) return RoleName.MARKETING;

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
    LoanApplication loan =
        loanApplicationRepository
            .findByIdWithDetails(loanId)
            .orElseThrow(() -> BusinessException.notFound("Loan application not found"));

    LoanStatus expectedStatus = getExpectedStatus(role);

    if (loan.getStatus() != expectedStatus) {
      throw BusinessException.badRequest(
          "Loan is not in the correct status for your approval. "
              + "Current status: "
              + loan.getStatus()
              + ", Expected: "
              + expectedStatus);
    }

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

  private void createHistoryEntry(
      LoanApplication loan, User approver, RoleName role, LoanStatus status, String note) {
    LoanApplicationHistory history =
        LoanApplicationHistory.builder()
            .loanApplication(loan)
            .approvedBy(approver)
            .approvedByRole(role.name())
            .approvedByBranchId(
                approver.getBranch() != null ? approver.getBranch().getId().intValue() : null)
            .status(status)
            .note(note)
            .build();

    historyRepository.save(history);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MyApprovalHistoryResponse> getMyApprovalHistory(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    return historyRepository.findByApprovedByIdWithLoanDetails(user.getId()).stream()
        .filter(h -> !h.getApprovedByRole().equals("CUSTOMER"))
        .map(loanApplicationMapper::toMyApprovalHistoryResponse)
        .collect(Collectors.toList());
  }
}
