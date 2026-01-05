package com.gvn.binarbe.entity;

import com.gvn.binarbe.enums.LoanStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * LoanApplicationHistory entity for tracking approval workflow. Contains snapshot data of approver
 * information at the time of action.
 */
@Entity
@Table(name = "loan_application_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplicationHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "loan_application_id", nullable = false)
  private LoanApplication loanApplication;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_by", nullable = false)
  private User approvedBy;

  /**
   * Snapshot of the role name at the time of approval. Stored as string to preserve historical
   * accuracy even if role changes.
   */
  @Column(name = "approved_by_role", nullable = false)
  private String approvedByRole;

  /**
   * Snapshot of the branch ID at the time of approval. Stored as integer to preserve historical
   * accuracy.
   */
  @Column(name = "approved_by_branch_id")
  private Integer approvedByBranchId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private LoanStatus status;

  @Column(length = 1000)
  private String note;

  @Column(name = "created_at", nullable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();
}
