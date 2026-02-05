package com.gvn.binarbe.entity;

import com.gvn.binarbe.enums.LoanStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/**
 * LoanApplication entity representing customer loan requests. Tracks status through multi-level
 * approval workflow. Contains snapshot of customer data at time of submission.
 */
@Entity
@Table(name = "loan_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private User customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "branch_id", nullable = false)
  private Branch branch;

  @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
  private java.math.BigDecimal requestedAmount;

  @Column(name = "requested_tenor", nullable = false)
  private Integer requestedTenor; // in months

  @Column(name = "requested_rate", nullable = false, precision = 5, scale = 2)
  private java.math.BigDecimal requestedRate; // percentage per annum

  @Column(name = "latitude")
  private String latitude;

  @Column(name = "longitude")
  private String longitude;

  // ==========================================
  // CUSTOMER SNAPSHOT (preserved at submission)
  // ==========================================

  @Column(name = "customer_name_snapshot", nullable = false)
  private String customerNameSnapshot;

  @Column(name = "customer_email_snapshot", nullable = false)
  private String customerEmailSnapshot;

  @Column(name = "customer_nik_snapshot", length = 16)
  private String customerNikSnapshot;

  @Column(name = "customer_phone_snapshot")
  private String customerPhoneSnapshot;

  @Column(name = "customer_address_snapshot", columnDefinition = "TEXT")
  private String customerAddressSnapshot;

  @Column(name = "customer_birthdate_snapshot")
  private LocalDate customerBirthdateSnapshot;

  @Column(name = "customer_ktp_path_snapshot")
  private String customerKtpPathSnapshot;

  @Column(name = "customer_kk_path_snapshot")
  private String customerKkPathSnapshot;

  @Column(name = "customer_npwp_path_snapshot")
  private String customerNpwpPathSnapshot;

  @Column(name = "customer_bank_name_snapshot", length = 100)
  private String customerBankNameSnapshot;

  @Column(name = "customer_account_number_snapshot", length = 30)
  private String customerAccountNumberSnapshot;

  @Column(name = "customer_account_holder_name_snapshot", length = 100)
  private String customerAccountHolderNameSnapshot;

  @Column(name = "customer_selfie_path_snapshot")
  private String customerSelfiePathSnapshot;

  @Column(name = "customer_salary_slip_path_snapshot")
  private String customerSalarySlipPathSnapshot;

  @Column(name = "customer_job_snapshot")
  private String customerJobSnapshot;

  @Column(name = "customer_company_name_snapshot")
  private String customerCompanyNameSnapshot;

  // ==========================================

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private LoanStatus status = LoanStatus.SUBMITTED;

  @Column(name = "created_at", nullable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  @OrderBy("createdAt ASC")
  private List<LoanApplicationHistory> histories = new ArrayList<>();

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
