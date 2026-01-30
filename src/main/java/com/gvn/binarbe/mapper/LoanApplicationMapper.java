package com.gvn.binarbe.mapper;

import com.gvn.binarbe.dto.response.LoanApplicationResponse;
import com.gvn.binarbe.dto.response.LoanHistoryResponse;
import com.gvn.binarbe.dto.response.MyApprovalHistoryResponse;
import com.gvn.binarbe.entity.LoanApplication;
import com.gvn.binarbe.entity.LoanApplicationHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoanApplicationMapper {

  private final BranchMapper branchMapper;
  private final ProductMapper productMapper;

  public LoanApplicationResponse toResponse(LoanApplication loan) {
    if (loan == null) return null;
    return LoanApplicationResponse.builder()
        .id(loan.getId())
        // Use snapshot data (preserved from submission time)
        .customerName(loan.getCustomerNameSnapshot())
        .customerEmail(loan.getCustomerEmailSnapshot())
        .customerNik(loan.getCustomerNikSnapshot())
        .customerPhone(loan.getCustomerPhoneSnapshot())
        .customerAddress(loan.getCustomerAddressSnapshot())
        .customerBirthdate(loan.getCustomerBirthdateSnapshot())
        .customerKtpPath(loan.getCustomerKtpPathSnapshot())
        .customerKkPath(loan.getCustomerKkPathSnapshot())
        .customerNpwpPath(loan.getCustomerNpwpPathSnapshot())
        .customerBankName(loan.getCustomerBankNameSnapshot())
        .customerAccountNumber(loan.getCustomerAccountNumberSnapshot())
        .customerAccountHolderName(loan.getCustomerAccountHolderNameSnapshot())
        .product(productMapper.toResponse(loan.getProduct()))
        .branch(branchMapper.toResponse(loan.getBranch()))
        .requestedAmount(loan.getRequestedAmount())
        .requestedTenor(loan.getRequestedTenor())
        .requestedRate(loan.getRequestedRate())
        .latitude(loan.getLatitude())
        .longitude(loan.getLongitude())
        .status(loan.getStatus())
        .createdAt(loan.getCreatedAt())
        .updatedAt(loan.getUpdatedAt())
        .build();
  }

  public LoanHistoryResponse toHistoryResponse(LoanApplicationHistory history) {
    if (history == null) return null;
    return LoanHistoryResponse.builder()
        .id(history.getId())
        .approvedByName(history.getApprovedBy().getName())
        .approvedByRole(history.getApprovedByRole())
        .approvedByBranch(
            history.getApprovedByBranchId() != null
                ? "Branch ID: " + history.getApprovedByBranchId()
                : "N/A")
        .status(history.getStatus())
        .note(history.getNote())
        .createdAt(history.getCreatedAt())
        .build();
  }

  public MyApprovalHistoryResponse toMyApprovalHistoryResponse(LoanApplicationHistory h) {
    if (h == null) return null;
    LoanApplication loan = h.getLoanApplication();
    return MyApprovalHistoryResponse.builder()
        .id(h.getId())
        .loanId(loan.getId())
        .customerName(loan.getCustomerNameSnapshot())
        .productName(loan.getProduct().getName())
        .loanAmount(loan.getRequestedAmount())
        .branchLocation(loan.getBranch().getLocation())
        .actionTaken(h.getStatus())
        .currentStatus(loan.getStatus())
        .note(h.getNote())
        .actionDate(h.getCreatedAt())
        .build();
  }
}
