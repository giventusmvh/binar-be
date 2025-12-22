package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.dto.request.LoginRequest;
import com.gvn.binarbe.dto.request.RegisterRequest;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of AuthService for authentication operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

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
                .build();
    }
}
