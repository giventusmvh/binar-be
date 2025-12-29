package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.dto.request.ForgotPasswordRequest;
import com.gvn.binarbe.dto.request.LoginRequest;
import com.gvn.binarbe.dto.request.RegisterRequest;
import com.gvn.binarbe.dto.request.ResetPasswordRequest;
import com.gvn.binarbe.dto.response.AuthResponse;
import com.gvn.binarbe.entity.Role;
import com.gvn.binarbe.entity.User;
import com.gvn.binarbe.entity.UserProfile;
import com.gvn.binarbe.enums.RoleName;
import com.gvn.binarbe.enums.UserType;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.repository.RoleRepository;
import com.gvn.binarbe.repository.UserProfileRepository;
import com.gvn.binarbe.repository.UserRepository;
import com.gvn.binarbe.security.JwtUtil;
import com.gvn.binarbe.service.AuthService;
import com.gvn.binarbe.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation of AuthService for authentication operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

        private static final String PASSWORD_RESET_KEY_PREFIX = "password-reset:";

        private final UserRepository userRepository;
        private final UserProfileRepository userProfileRepository;
        private final RoleRepository roleRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;
        private final AuthenticationManager authenticationManager;
        private final UserDetailsService userDetailsService;
        private final EmailService emailService;
        private final StringRedisTemplate redisTemplate;

        @Value("${app.password-reset.token-expiry-minutes}")
        private int tokenExpiryMinutes;

        @Override
        @Transactional
        public AuthResponse register(RegisterRequest request) {
                log.info("Registering new customer: {}", request.getEmail());

                // Check if email already exists
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw BusinessException.conflict("Email already registered");
                }

                // Get CUSTOMER role
                Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                                .orElseThrow(() -> BusinessException.notFound("Customer role not found"));

                // Create new user
                Set<Role> roles = new HashSet<>();
                roles.add(customerRole);

                User user = User.builder()
                                .name(request.getName())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .userType(UserType.CUSTOMER)
                                .isActive(true)
                                .roles(roles)
                                .build();

                user = userRepository.save(user);

                // Create empty profile
                UserProfile profile = UserProfile.builder()
                                .user(user)
                                .build();
                userProfileRepository.save(profile);

                // Generate token
                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                String token = jwtUtil.generateToken(userDetails);

                log.info("Customer registered successfully: {}", user.getEmail());

                return buildAuthResponse(user, token);
        }

        @Override
        public AuthResponse login(LoginRequest request) {
                log.info("Authenticating user: {}", request.getEmail());

                // Authenticate
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                // Get user
                User user = userRepository.findByEmailWithRoles(request.getEmail())
                                .orElseThrow(() -> BusinessException.unauthorized("Invalid credentials"));

                if (!user.getIsActive()) {
                        throw BusinessException.unauthorized("Account is disabled");
                }

                // Generate token
                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                String token = jwtUtil.generateToken(userDetails);

                log.info("User authenticated successfully: {}", user.getEmail());

                return buildAuthResponse(user, token);
        }

        @Override
        public void forgotPassword(ForgotPasswordRequest request) {
                log.info("Processing forgot password request for: {}", request.getEmail());

                // Find user by email (don't reveal if email exists for security)
                User user = userRepository.findByEmail(request.getEmail())
                                .orElse(null);

                if (user == null) {
                        // Log but don't reveal to client that email doesn't exist
                        log.warn("Forgot password requested for non-existent email: {}", request.getEmail());
                        return;
                }

                // Generate reset token
                String resetToken = UUID.randomUUID().toString();
                String redisKey = PASSWORD_RESET_KEY_PREFIX + resetToken;

                // Store token in Redis with TTL
                redisTemplate.opsForValue().set(
                                redisKey,
                                user.getId().toString(),
                                tokenExpiryMinutes,
                                TimeUnit.MINUTES);

                // Send email with reset link
                emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

                log.info("Password reset email sent to: {}", user.getEmail());
        }

        @Override
        @Transactional
        public void resetPassword(ResetPasswordRequest request) {
                log.info("Processing password reset with token");

                // Validate passwords match
                if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                        throw BusinessException.badRequest("New password and confirm password do not match");
                }

                // Get user ID from Redis
                String redisKey = PASSWORD_RESET_KEY_PREFIX + request.getToken();
                String userIdStr = redisTemplate.opsForValue().get(redisKey);

                if (userIdStr == null) {
                        throw BusinessException.badRequest("Invalid or expired reset token");
                }

                // Find user
                Long userId = Long.parseLong(userIdStr);
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> BusinessException.notFound("User not found"));

                // Update password
                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                userRepository.save(user);

                // Delete token from Redis (one-time use)
                redisTemplate.delete(redisKey);

                log.info("Password reset successfully for user ID: {}", userId);
        }

        private AuthResponse buildAuthResponse(User user, String token) {
                return AuthResponse.builder()
                                .token(token)
                                .tokenType("Bearer")
                                .userId(user.getId())
                                .email(user.getEmail())
                                .name(user.getName())
                                .roles(user.getRoles().stream()
                                                .map(role -> role.getName().name())
                                                .collect(Collectors.toList()))
                                .permissions(user.getRoles().stream()
                                                .flatMap(role -> role.getPermissions().stream())
                                                .map(permission -> permission.getCode())
                                                .distinct()
                                                .collect(Collectors.toList()))
                                .build();
        }
}
