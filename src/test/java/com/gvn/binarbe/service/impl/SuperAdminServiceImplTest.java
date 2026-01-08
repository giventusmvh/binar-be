package com.gvn.binarbe.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.gvn.binarbe.dto.response.UserResponse;
import com.gvn.binarbe.entity.User;
import com.gvn.binarbe.entity.UserProfile;
import com.gvn.binarbe.enums.UserType;
import com.gvn.binarbe.mapper.UserMapper;
import com.gvn.binarbe.repository.BranchRepository;
import com.gvn.binarbe.repository.PermissionRepository;
import com.gvn.binarbe.repository.RoleRepository;
import com.gvn.binarbe.repository.UserRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SuperAdminServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PermissionRepository permissionRepository;
  @Mock private BranchRepository branchRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private UserMapper userMapper;

  @InjectMocks private SuperAdminServiceImpl superAdminService;

  private User testUser;
  private UserProfile testProfile;

  @BeforeEach
  void setUp() {
    testUser =
        User.builder()
            .id(1L)
            .name("Test User")
            .email("test@example.com")
            .userType(UserType.CUSTOMER)
            .isActive(true)
            .roles(new HashSet<>())
            .build();

    testProfile =
        UserProfile.builder()
            .id(1L)
            .user(testUser)
            .birthdate(LocalDate.of(1990, 1, 1))
            .phone("08123456789")
            .address("Some Address")
            .nik("1234567890123456")
            .ktpPath("ktp.jpg")
            .kkPath("kk.jpg")
            .npwpPath("npwp.jpg")
            .build();

    testUser.setProfile(testProfile);
  }

  @Test
  @DisplayName("Should return user list with mapped response")
  void getAllUsers_ShouldReturnUserList() {
    // Arrange
    when(userRepository.findAll()).thenReturn(Collections.singletonList(testUser));

    UserResponse mockResponse = UserResponse.builder().id(1L).name("Test User").build();

    when(userMapper.toUserResponse(testUser)).thenReturn(mockResponse);

    // Act
    List<UserResponse> responses = superAdminService.getAllUsers();

    // Assert
    assertNotNull(responses);
    assertEquals(1, responses.size());
    assertEquals(mockResponse, responses.get(0));
  }
}
