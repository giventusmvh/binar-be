package com.gvn.binarbe.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.gvn.binarbe.dto.request.LoginRequest;
import com.gvn.binarbe.dto.request.RegisterRequest;
import com.gvn.binarbe.dto.response.AuthResponse;
import com.gvn.binarbe.entity.Permission;
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
import com.gvn.binarbe.service.EmailService;
import com.gvn.binarbe.service.TokenBlacklistService;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests for AuthServiceImpl. */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock private UserRepository userRepository;

  @Mock private UserProfileRepository userProfileRepository;

  @Mock private RoleRepository roleRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private JwtUtil jwtUtil;

  @Mock private AuthenticationManager authenticationManager;

  @Mock private UserDetailsService userDetailsService;

  @Mock private EmailService emailService;

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private TokenBlacklistService tokenBlacklistService;

  @InjectMocks private AuthServiceImpl authService;

  private Role customerRole;
  private User testUser;
  private Permission viewPermission;

  @BeforeEach
  void setUp() {
    // Setup permission
    viewPermission = Permission.builder().id(1L).code("VIEW_PROFILE").build();

    Set<Permission> permissions = new HashSet<>();
    permissions.add(viewPermission);

    // Setup customer role
    customerRole = Role.builder().id(1L).name(RoleName.CUSTOMER).permissions(permissions).build();

    // Setup test user
    Set<Role> roles = new HashSet<>();
    roles.add(customerRole);

    testUser =
        User.builder()
            .id(1L)
            .name("Test User")
            .email("test@example.com")
            .password("encodedPassword")
            .userType(UserType.CUSTOMER)
            .isActive(true)
            .roles(roles)
            .build();
  }

  @Nested
  @DisplayName("Register Tests")
  class RegisterTests {

    @Test
    @DisplayName("Should register new customer successfully")
    void register_Success() {
      // Arrange
      RegisterRequest request =
          RegisterRequest.builder()
              .name("New User")
              .email("newuser@example.com")
              .password("password123")
              .build();

      when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
      when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
      when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
      when(userRepository.save(any(User.class)))
          .thenAnswer(
              invocation -> {
                User savedUser = invocation.getArgument(0);
                savedUser.setId(1L);
                return savedUser;
              });
      when(userProfileRepository.save(any(UserProfile.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      UserDetails mockUserDetails = mock(UserDetails.class);
      when(userDetailsService.loadUserByUsername(request.getEmail())).thenReturn(mockUserDetails);
      when(jwtUtil.generateToken(mockUserDetails)).thenReturn("jwt-token");

      // Act
      AuthResponse response = authService.register(request);

      // Assert
      assertNotNull(response);
      assertEquals("jwt-token", response.getToken());
      assertEquals("Bearer", response.getTokenType());
      assertEquals("New User", response.getName());
      assertEquals("newuser@example.com", response.getEmail());
      assertNotNull(response.getRoles());
      assertTrue(response.getRoles().contains("CUSTOMER"));

      verify(userRepository).existsByEmail(request.getEmail());
      verify(roleRepository).findByName(RoleName.CUSTOMER);
      verify(passwordEncoder).encode(request.getPassword());
      verify(userRepository).save(any(User.class));
      verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void register_EmailAlreadyExists_ThrowsException() {
      // Arrange
      RegisterRequest request =
          RegisterRequest.builder()
              .name("New User")
              .email("existing@example.com")
              .password("password123")
              .build();

      when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

      // Act & Assert
      BusinessException exception =
          assertThrows(BusinessException.class, () -> authService.register(request));

      assertEquals("Email already registered", exception.getMessage());
      verify(userRepository).existsByEmail(request.getEmail());
      verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when customer role not found")
    void register_CustomerRoleNotFound_ThrowsException() {
      // Arrange
      RegisterRequest request =
          RegisterRequest.builder()
              .name("New User")
              .email("newuser@example.com")
              .password("password123")
              .build();

      when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
      when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.empty());

      // Act & Assert
      BusinessException exception =
          assertThrows(BusinessException.class, () -> authService.register(request));

      assertEquals("Customer role not found", exception.getMessage());
      verify(userRepository, never()).save(any(User.class));
    }
  }

  @Nested
  @DisplayName("Login Tests")
  class LoginTests {

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_Success() {
      // Arrange
      LoginRequest request =
          LoginRequest.builder().email("test@example.com").password("password123").build();

      when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
          .thenReturn(new UsernamePasswordAuthenticationToken(request.getEmail(), null));
      when(userRepository.findByEmailWithRoles(request.getEmail()))
          .thenReturn(Optional.of(testUser));

      UserDetails mockUserDetails = mock(UserDetails.class);
      when(userDetailsService.loadUserByUsername(request.getEmail())).thenReturn(mockUserDetails);
      when(jwtUtil.generateToken(mockUserDetails)).thenReturn("jwt-token");

      // Act
      AuthResponse response = authService.login(request);

      // Assert
      assertNotNull(response);
      assertEquals("jwt-token", response.getToken());
      assertEquals("Bearer", response.getTokenType());
      assertEquals(1L, response.getUserId());
      assertEquals("test@example.com", response.getEmail());
      assertEquals("Test User", response.getName());
      assertTrue(response.getRoles().contains("CUSTOMER"));
      assertTrue(response.getPermissions().contains("VIEW_PROFILE"));

      verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
      verify(userRepository).findByEmailWithRoles(request.getEmail());
    }

    @Test
    @DisplayName("Should throw exception when credentials are invalid")
    void login_InvalidCredentials_ThrowsException() {
      // Arrange
      LoginRequest request =
          LoginRequest.builder().email("test@example.com").password("wrongpassword").build();

      when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
          .thenThrow(new BadCredentialsException("Bad credentials"));

      // Act & Assert
      assertThrows(BadCredentialsException.class, () -> authService.login(request));

      verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
      verify(userRepository, never()).findByEmailWithRoles(anyString());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void login_UserNotFound_ThrowsException() {
      // Arrange
      LoginRequest request =
          LoginRequest.builder().email("notfound@example.com").password("password123").build();

      when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
          .thenReturn(new UsernamePasswordAuthenticationToken(request.getEmail(), null));
      when(userRepository.findByEmailWithRoles(request.getEmail())).thenReturn(Optional.empty());

      // Act & Assert
      BusinessException exception =
          assertThrows(BusinessException.class, () -> authService.login(request));

      assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when user account is disabled")
    void login_AccountDisabled_ThrowsException() {
      // Arrange
      LoginRequest request =
          LoginRequest.builder().email("test@example.com").password("password123").build();

      testUser.setIsActive(false);

      when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
          .thenReturn(new UsernamePasswordAuthenticationToken(request.getEmail(), null));
      when(userRepository.findByEmailWithRoles(request.getEmail()))
          .thenReturn(Optional.of(testUser));

      // Act & Assert
      BusinessException exception =
          assertThrows(BusinessException.class, () -> authService.login(request));

      assertEquals("Account is disabled", exception.getMessage());
      verify(userRepository).findByEmailWithRoles(request.getEmail());
      verify(jwtUtil, never()).generateToken(any());
    }
  }

  @Nested
  @DisplayName("Logout Tests")
  class LogoutTests {

    @Test
    @DisplayName("Should logout successfully")
    void logout_Success() {
      // Arrange
      String token = "valid-jwt-token";
      java.util.Date futureDate =
          new java.util.Date(System.currentTimeMillis() + 3600000); // 1 hour from now

      when(jwtUtil.extractExpiration(token)).thenReturn(futureDate);
      doNothing().when(tokenBlacklistService).blacklistToken(anyString(), anyLong());

      // Act
      authService.logout(token);

      // Assert
      verify(jwtUtil).extractExpiration(token);
      verify(tokenBlacklistService).blacklistToken(eq(token), anyLong());
    }

    @Test
    @DisplayName("Should handle exception during logout gracefully")
    void logout_Exception_HandledGracefully() {
      // Arrange
      String token = "invalid-token";

      when(jwtUtil.extractExpiration(token)).thenThrow(new RuntimeException("Token parsing error"));

      // Act - should not throw exception
      assertDoesNotThrow(() -> authService.logout(token));

      // Assert
      verify(jwtUtil).extractExpiration(token);
      verify(tokenBlacklistService, never()).blacklistToken(anyString(), anyLong());
    }
  }
}
