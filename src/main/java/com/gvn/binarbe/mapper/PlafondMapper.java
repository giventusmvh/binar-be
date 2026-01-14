package com.gvn.binarbe.mapper;

import com.gvn.binarbe.dto.response.UserPlafondResponse;
import com.gvn.binarbe.entity.UserPlafond;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlafondMapper {

  private final ProductMapper productMapper;

  public UserPlafondResponse toResponse(UserPlafond userPlafond) {
    if (userPlafond == null) return null;
    return UserPlafondResponse.builder()
        .id(userPlafond.getId())
        .product(productMapper.toResponse(userPlafond.getProduct()))
        .originalAmount(userPlafond.getProduct().getAmount())
        .remainingAmount(userPlafond.getRemainingAmount())
        .assignedAt(userPlafond.getAssignedAt())
        .isActive(userPlafond.getIsActive())
        .build();
  }
}
