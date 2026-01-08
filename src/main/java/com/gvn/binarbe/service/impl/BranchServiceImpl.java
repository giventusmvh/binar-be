package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.dto.request.CreateBranchRequest;
import com.gvn.binarbe.dto.request.UpdateBranchRequest;
import com.gvn.binarbe.dto.response.BranchResponse;
import com.gvn.binarbe.entity.Branch;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.repository.BranchRepository;
import com.gvn.binarbe.service.BranchService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchServiceImpl implements BranchService {

  private final BranchRepository branchRepository;

  @Override
  @Transactional(readOnly = true)
  public List<BranchResponse> getAllBranches() {
    return branchRepository.findAll().stream()
        .map(this::mapToBranchResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public BranchResponse getBranchById(Long id) {
    Branch branch =
        branchRepository
            .findById(id)
            .orElseThrow(() -> BusinessException.notFound("Branch not found"));
    return mapToBranchResponse(branch);
  }

  @Override
  @Transactional
  public BranchResponse createBranch(CreateBranchRequest request) {
    log.info("Creating new branch with code: {}", request.getCode());

    if (branchRepository.existsByCode(request.getCode())) {
      throw BusinessException.conflict("Branch code already exists");
    }

    Branch branch =
        Branch.builder().code(request.getCode()).location(request.getLocation()).build();

    branch = branchRepository.save(branch);
    log.info("Branch created successfully with ID: {}", branch.getId());

    return mapToBranchResponse(branch);
  }

  @Override
  @Transactional
  public BranchResponse updateBranch(Long id, UpdateBranchRequest request) {
    log.info("Updating branch with ID: {}", id);

    Branch branch =
        branchRepository
            .findById(id)
            .orElseThrow(() -> BusinessException.notFound("Branch not found"));

    if (request.getCode() != null && !request.getCode().isBlank()) {
      if (!request.getCode().equals(branch.getCode())) {
        if (branchRepository.existsByCode(request.getCode())) {
          throw BusinessException.conflict("Branch code already exists");
        }
        branch.setCode(request.getCode());
      }
    }

    if (request.getLocation() != null && !request.getLocation().isBlank()) {
      branch.setLocation(request.getLocation());
    }

    branch = branchRepository.save(branch);
    log.info("Branch updated successfully");

    return mapToBranchResponse(branch);
  }

  @Override
  @Transactional
  public void deleteBranch(Long id) {
    log.info("Deleting branch with ID: {}", id);

    Branch branch =
        branchRepository
            .findById(id)
            .orElseThrow(() -> BusinessException.notFound("Branch not found"));

    // Check if branch has users or loan applications before deleting (constraint
    // check)
    if (!branch.getUsers().isEmpty()) {
      throw BusinessException.conflict("Cannot delete branch that has assigned users");
    }

    // Note: Checking loan applications might be needed too depending on cascade
    // rules,
    // but starting with users is safe.
    // Ideally we catch DataIntegrityViolationException but explicit check is
    // friendlier.

    branchRepository.delete(branch);
    log.info("Branch deleted successfully");
  }

  private BranchResponse mapToBranchResponse(Branch branch) {
    return BranchResponse.builder()
        .id(branch.getId())
        .code(branch.getCode())
        .location(branch.getLocation())
        .build();
  }
}
