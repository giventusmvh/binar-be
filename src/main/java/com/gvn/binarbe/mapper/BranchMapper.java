package com.gvn.binarbe.mapper;

import com.gvn.binarbe.dto.response.BranchResponse;
import com.gvn.binarbe.entity.Branch;
import org.springframework.stereotype.Component;

@Component
public class BranchMapper {

  public BranchResponse toResponse(Branch branch) {
    if (branch == null) return null;
    return BranchResponse.builder()
        .id(branch.getId())
        .code(branch.getCode())
        .location(branch.getLocation())
        .build();
  }
}
