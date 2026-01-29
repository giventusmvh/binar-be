package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.dto.request.UpdateProfileRequest;
import com.gvn.binarbe.dto.response.BranchResponse;
import com.gvn.binarbe.dto.response.ProductResponse;
import com.gvn.binarbe.dto.response.UserProfileResponse;
import com.gvn.binarbe.dto.response.UserResponse;
import com.gvn.binarbe.entity.User;
import com.gvn.binarbe.entity.UserProfile;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.mapper.BranchMapper;
import com.gvn.binarbe.mapper.ProductMapper;
import com.gvn.binarbe.mapper.UserMapper;
import com.gvn.binarbe.repository.BranchRepository;
import com.gvn.binarbe.repository.ProductRepository;
import com.gvn.binarbe.repository.UserProfileRepository;
import com.gvn.binarbe.repository.UserRepository;
import com.gvn.binarbe.service.CustomerService;
import com.gvn.binarbe.service.FileStorageService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Implementation of CustomerService for customer operations. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final ProductRepository productRepository;
  private final BranchRepository branchRepository;
  private final FileStorageService fileStorageService;
  private final UserMapper userMapper;
  private final BranchMapper branchMapper;
  private final ProductMapper productMapper;

  @Override
  @Transactional(readOnly = true)
  public UserResponse getProfile(String email) {
    User user =
        userRepository
            .findByIdWithRolesAndProfile(
                userRepository
                    .findByEmail(email)
                    .orElseThrow(() -> BusinessException.notFound("User not found"))
                    .getId())
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    return userMapper.toUserResponse(user);
  }

  @Override
  @Transactional
  public UserProfileResponse updateProfile(
      String email,
      UpdateProfileRequest request,
      MultipartFile ktp,
      MultipartFile kk,
      MultipartFile npwp) {
    log.info("Updating profile for user: {}", email);

    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    UserProfile profile =
        userProfileRepository
            .findByUserId(user.getId())
            .orElseGet(() -> UserProfile.builder().user(user).build());

    profile.setBirthdate(request.getBirthdate());
    profile.setPhone(request.getPhone());
    profile.setAddress(request.getAddress());
    profile.setNik(request.getNik());
    profile.setBankName(request.getBankName());
    profile.setAccountNumber(request.getAccountNumber());
    profile.setAccountHolderName(request.getAccountHolderName());

    if (ktp != null && !ktp.isEmpty()) {
      String ktpFileName = fileStorageService.storeFile(ktp);
      profile.setKtpPath(ktpFileName);
    }

    if (kk != null && !kk.isEmpty()) {
      String kkFileName = fileStorageService.storeFile(kk);
      profile.setKkPath(kkFileName);
    }

    if (npwp != null && !npwp.isEmpty()) {
      String npwpFileName = fileStorageService.storeFile(npwp);
      profile.setNpwpPath(npwpFileName);
    }

    profile = userProfileRepository.save(profile);

    log.info("Profile updated for user: {}", email);

    return userMapper.toProfileResponse(profile);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isProfileComplete(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    return userProfileRepository
        .findByUserId(user.getId())
        .map(UserProfile::isComplete)
        .orElse(false);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProductResponse> getAllProducts() {
    return productRepository.findAll().stream()
        .map(productMapper::toResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<BranchResponse> getAllBranches() {
    return branchRepository.findAll().stream()
        .map(branchMapper::toResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void registerFcmToken(String email, String fcmToken) {
    log.info("Registering FCM token for user: {}", email);
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    user.setFcmToken(fcmToken);
    userRepository.save(user);
    log.info("FCM token registered for user: {}", email);
  }
}
