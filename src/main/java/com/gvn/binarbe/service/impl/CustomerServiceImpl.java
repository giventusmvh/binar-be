package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.dto.request.UpdateProfileRequest;
import com.gvn.binarbe.dto.response.BranchResponse;
import com.gvn.binarbe.dto.response.ProductResponse;
import com.gvn.binarbe.dto.response.UserDocumentResponse;
import com.gvn.binarbe.dto.response.UserProfileResponse;
import com.gvn.binarbe.dto.response.UserResponse;
import com.gvn.binarbe.entity.Branch;
import com.gvn.binarbe.entity.Product;
import com.gvn.binarbe.entity.User;
import com.gvn.binarbe.entity.UserDocument;
import com.gvn.binarbe.entity.UserProfile;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.repository.BranchRepository;
import com.gvn.binarbe.repository.ProductRepository;
import com.gvn.binarbe.repository.UserDocumentRepository;
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
  private final UserDocumentRepository userDocumentRepository;

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

    return mapToUserResponse(user);
  }

  @Override
  @Transactional
  public UserProfileResponse updateProfile(
      String email, UpdateProfileRequest request, List<MultipartFile> files) {
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

    profile = userProfileRepository.save(profile);

    if (files != null && !files.isEmpty()) {
      for (MultipartFile file : files) {
        String fileName = fileStorageService.storeFile(file);

        UserDocument document =
            UserDocument.builder()
                .user(user)
                .fileName(file.getOriginalFilename())
                .filePath(fileName)
                .fileType(file.getContentType())
                .build();

        userDocumentRepository.save(document);
      }
    }

    log.info("Profile updated for user: {}", email);

    return mapToProfileResponse(profile);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isProfileComplete(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> BusinessException.notFound("User not found"));

    boolean isProfileFieldsComplete =
        userProfileRepository.findByUserId(user.getId()).map(UserProfile::isComplete).orElse(false);

    boolean hasDocuments = !userDocumentRepository.findByUserId(user.getId()).isEmpty();

    return isProfileFieldsComplete && hasDocuments;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProductResponse> getAllProducts() {
    return productRepository.findAll().stream()
        .map(this::mapToProductResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<BranchResponse> getAllBranches() {
    return branchRepository.findAll().stream()
        .map(this::mapToBranchResponse)
        .collect(Collectors.toList());
  }

  private UserResponse mapToUserResponse(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .name(user.getName())
        .email(user.getEmail())
        .userType(user.getUserType())
        .isActive(user.getIsActive())
        .branch(user.getBranch() != null ? mapToBranchResponse(user.getBranch()) : null)
        .profile(user.getProfile() != null ? mapToProfileResponse(user.getProfile()) : null)
        .roles(
            user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList()))
        .createdAt(user.getCreatedAt())
        .build();
  }

  private UserProfileResponse mapToProfileResponse(UserProfile profile) {
    return UserProfileResponse.builder()
        .id(profile.getId())
        .birthdate(profile.getBirthdate())
        .phone(profile.getPhone())
        .address(profile.getAddress())
        .nik(profile.getNik())
        .isComplete(profile.isComplete())
        .documents(
            userDocumentRepository.findByUserId(profile.getUser().getId()).stream()
                .map(this::mapToDocumentResponse)
                .collect(Collectors.toList()))
        .build();
  }

  private UserDocumentResponse mapToDocumentResponse(UserDocument document) {
    return UserDocumentResponse.builder()
        .id(document.getId())
        .fileName(document.getFileName())
        .fileType(document.getFileType())
        .url("/uploads/" + document.getFilePath())
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
