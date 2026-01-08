package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.dto.response.UserResponse;
import com.gvn.binarbe.entity.User;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.mapper.UserMapper;
import com.gvn.binarbe.repository.UserRepository;
import com.gvn.binarbe.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Override
  @Transactional(readOnly = true)
  public UserResponse getUserById(Long userId) {
    User user =
        userRepository
            .findByIdWithRolesAndProfile(userId)
            .orElseThrow(() -> BusinessException.notFound("User not found"));
    return userMapper.toUserResponse(user);
  }
}
