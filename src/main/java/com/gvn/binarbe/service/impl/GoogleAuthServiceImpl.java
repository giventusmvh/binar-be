package com.gvn.binarbe.service.impl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.gvn.binarbe.dto.request.GoogleLoginRequest;
import com.gvn.binarbe.dto.response.AuthResponse;
import com.gvn.binarbe.entity.Role;
import com.gvn.binarbe.entity.User;
import com.gvn.binarbe.entity.UserProfile;
import com.gvn.binarbe.enums.RoleName;
import com.gvn.binarbe.enums.UserType;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.mapper.AuthMapper;
import com.gvn.binarbe.repository.RoleRepository;
import com.gvn.binarbe.repository.UserProfileRepository;
import com.gvn.binarbe.repository.UserRepository;
import com.gvn.binarbe.security.JwtUtil;
import com.gvn.binarbe.service.GoogleAuthService;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of GoogleAuthService using Firebase Authentication. */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthServiceImpl implements GoogleAuthService {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final RoleRepository roleRepository;
  private final JwtUtil jwtUtil;
  private final UserDetailsService userDetailsService;
  private final AuthMapper authMapper;

  @Override
  @Transactional
  public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
    log.info("Processing Firebase authentication");

    // Verify Firebase ID token
    FirebaseToken decodedToken = verifyFirebaseToken(request.getIdToken());

    String email = decodedToken.getEmail();
    String name = decodedToken.getName();
    String uid = decodedToken.getUid();

    if (email == null || email.isEmpty()) {
      log.error("Firebase token does not contain email");
      throw BusinessException.unauthorized("Invalid Firebase token: email not found");
    }

    log.info("Firebase auth attempt for email: {}", email);

    // Find or create user
    Optional<User> existingUser = userRepository.findByEmail(email);
    User user;

    if (existingUser.isPresent()) {
      user = existingUser.get();
      log.info("Existing user found: {}", email);

      // Update FCM token if provided
      if (request.getFcmToken() != null && !request.getFcmToken().isEmpty()) {
        user.setFcmToken(request.getFcmToken());
        user = userRepository.save(user);
      }
    } else {
      // Create new customer user
      user = createNewFirebaseUser(email, name, request.getFcmToken());
      log.info("New user created from Firebase auth: {}", email);
    }

    if (!user.getIsActive()) {
      throw BusinessException.unauthorized("Account is disabled");
    }

    // Generate JWT token
    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
    String token = jwtUtil.generateToken(userDetails);

    log.info("Firebase authentication successful for: {}", email);

    return authMapper.toAuthResponse(user, token);
  }

  /**
   * Verifies the Firebase ID token and returns the decoded token.
   *
   * @param idToken Firebase ID token from Android client
   * @return FirebaseToken containing user information
   * @throws BusinessException if token is invalid
   */
  private FirebaseToken verifyFirebaseToken(String idToken) {
    try {
      FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
      FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);

      log.debug("Firebase token verified successfully for UID: {}", decodedToken.getUid());
      return decodedToken;
    } catch (FirebaseAuthException e) {
      log.error("Firebase token verification failed: {}", e.getMessage());
      throw BusinessException.unauthorized("Invalid Firebase ID token: " + e.getMessage());
    } catch (Exception e) {
      log.error("Error verifying Firebase token: {}", e.getMessage());
      throw BusinessException.unauthorized("Failed to verify Firebase ID token");
    }
  }

  /**
   * Creates a new user from Firebase authentication data.
   *
   * @param email user's email from Firebase
   * @param name user's name from Firebase
   * @param fcmToken optional FCM token
   * @return newly created User
   */
  private User createNewFirebaseUser(String email, String name, String fcmToken) {
    Role customerRole =
        roleRepository
            .findByName(RoleName.CUSTOMER)
            .orElseThrow(() -> BusinessException.notFound("Customer role not found"));

    Set<Role> roles = new HashSet<>();
    roles.add(customerRole);

    // Generate a random password since it's required but won't be used for Firebase
    // users
    String randomPassword = UUID.randomUUID().toString();

    User user =
        User.builder()
            .name(name != null ? name : email)
            .email(email)
            .password(randomPassword) // Required field, but not used for Firebase auth
            .userType(UserType.CUSTOMER)
            .isActive(true)
            .fcmToken(fcmToken)
            .roles(roles)
            .build();

    user = userRepository.save(user);

    // Create empty user profile
    UserProfile profile = UserProfile.builder().user(user).build();
    userProfileRepository.save(profile);

    return user;
  }
}
