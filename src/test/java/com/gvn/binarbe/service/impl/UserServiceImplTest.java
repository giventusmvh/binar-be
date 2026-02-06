package com.gvn.binarbe.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gvn.binarbe.dto.response.UserResponse;
import com.gvn.binarbe.entity.User;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.mapper.UserMapper;
import com.gvn.binarbe.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserRepository userRepository;

  @Mock private UserMapper userMapper;

  @InjectMocks private UserServiceImpl userService;

  @Test
  void getUserById_Success() {
    // Arrange
    Long userId = 1L;
    User user = new User();
    user.setId(userId);
    user.setEmail("test@example.com");

    UserResponse userResponse = new UserResponse();
    userResponse.setId(userId);
    userResponse.setEmail("test@example.com");

    when(userRepository.findByIdWithRolesAndProfile(userId)).thenReturn(Optional.of(user));
    when(userMapper.toUserResponse(user)).thenReturn(userResponse);

    // Act
    UserResponse result = userService.getUserById(userId);

    // Assert
    assertNotNull(result);
    assertEquals(userId, result.getId());
    assertEquals("test@example.com", result.getEmail());
    verify(userRepository).findByIdWithRolesAndProfile(userId);
    verify(userMapper).toUserResponse(user);
  }

  @Test
  void getUserById_NotFound() {
    // Arrange
    Long userId = 1L;
    when(userRepository.findByIdWithRolesAndProfile(userId)).thenReturn(Optional.empty());

    // Act & Assert
    BusinessException exception =
        assertThrows(BusinessException.class, () -> userService.getUserById(userId));

    assertEquals("User not found", exception.getMessage());
    verify(userRepository).findByIdWithRolesAndProfile(userId);
  }
}
