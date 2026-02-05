package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.dto.request.SelectPlafondRequest;
import com.gvn.binarbe.dto.response.UserPlafondResponse;
import com.gvn.binarbe.entity.LoanApplication;
import com.gvn.binarbe.entity.Product;
import com.gvn.binarbe.entity.User;
import com.gvn.binarbe.entity.UserPlafond;
import com.gvn.binarbe.entity.UserProfile;
import com.gvn.binarbe.enums.LoanStatus;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.mapper.PlafondMapper;
import com.gvn.binarbe.repository.LoanApplicationRepository;
import com.gvn.binarbe.repository.ProductRepository;
import com.gvn.binarbe.repository.UserPlafondRepository;
import com.gvn.binarbe.repository.UserProfileRepository;
import com.gvn.binarbe.repository.UserRepository;
import com.gvn.binarbe.service.PlafondService;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of PlafondService for credit limit operations. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlafondServiceImpl implements PlafondService {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final ProductRepository productRepository;
  private final UserPlafondRepository userPlafondRepository;
  private final LoanApplicationRepository loanApplicationRepository;
  private final PlafondMapper plafondMapper;

  @Override
  @Transactional
  public UserPlafondResponse selectPlafond(String email, SelectPlafondRequest request) {
    log.info("Selecting plafond for user: {}", email);

    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    UserProfile profile =
        userProfileRepository
            .findByUserId(user.getId())
            .orElseThrow(
                () ->
                    BusinessException.badRequest(
                        "Profile not found. Please complete your profile first."));

    if (!profile.isComplete()) {
      throw BusinessException.badRequest(
          "Please complete your profile before selecting a plafond. "
              + "Required fields: NIK, birthdate, phone, and address.");
    }

    if (userPlafondRepository.existsByUserIdAndIsActiveTrue(user.getId())) {
      throw BusinessException.badRequest(
          "You already have an active plafond. Cannot select another one.");
    }

    Product product =
        productRepository
            .findById(request.getProductId())
            .orElseThrow(() -> BusinessException.notFound("Product not found"));

    UserPlafond userPlafond =
        UserPlafond.builder()
            .user(user)
            .product(product)
            .remainingAmount(product.getAmount())
            .isActive(true)
            .build();

    userPlafond = userPlafondRepository.save(userPlafond);

    log.info("Plafond selected: User={}, Product={}", email, product.getName());

    return plafondMapper.toResponse(userPlafond);
  }

  @Override
  @Transactional(readOnly = true)
  public UserPlafondResponse getMyPlafond(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    UserPlafond userPlafond =
        userPlafondRepository
            .findByUserIdWithProduct(user.getId())
            .orElseThrow(
                () ->
                    BusinessException.notFound(
                        "You don't have an active plafond. Please select a plafond first."));

    return plafondMapper.toResponse(userPlafond);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasActivePlafond(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    return userPlafondRepository.existsByUserIdAndIsActiveTrue(user.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public UserPlafondResponse getUserPlafond(Long userId) {
    UserPlafond userPlafond =
        userPlafondRepository
            .findByUserIdWithProduct(userId)
            .orElseThrow(() -> BusinessException.notFound("User does not have any plafond"));

    return plafondMapper.toResponse(userPlafond);
  }

  @Override
  @Transactional
  public UserPlafondResponse deactivateUserPlafond(Long userId) {
    UserPlafond userPlafond =
        userPlafondRepository
            .findByUserIdWithProduct(userId)
            .orElseThrow(
                () ->
                    BusinessException.notFound(
                        "User does not have any active plafond to deactivate"));

    if (!userPlafond.getIsActive()) {
      throw BusinessException.badRequest("User plafond is already inactive");
    }

    userPlafond.setIsActive(false);
    userPlafond = userPlafondRepository.save(userPlafond);

    // Auto-reject pending loans
    List<LoanStatus> pendingStatuses =
        Arrays.asList(
            LoanStatus.SUBMITTED,
            LoanStatus.MARKETING_APPROVED,
            LoanStatus.BRANCH_MANAGER_APPROVED);

    List<LoanApplication> pendingLoans =
        loanApplicationRepository.findByCustomerId(userId).stream()
            .filter(loan -> pendingStatuses.contains(loan.getStatus()))
            .toList();

    for (LoanApplication loan : pendingLoans) {
      loan.setStatus(LoanStatus.REJECTED);
      loanApplicationRepository.save(loan);
      log.info("Auto-rejected loan application ID {} due to plafond deactivation", loan.getId());
    }

    log.info("Plafond deactivated for user ID: {}", userId);

    return plafondMapper.toResponse(userPlafond);
  }
}
