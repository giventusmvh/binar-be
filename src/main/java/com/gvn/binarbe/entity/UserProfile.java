package com.gvn.binarbe.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

/**
 * UserProfile entity containing additional customer information. Required fields must be completed
 * before submitting loan applications.
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column private LocalDate birthdate;

  @Column(length = 20)
  private String phone;

  @Column(length = 500)
  private String address;

  @Column(length = 16)
  private String nik; // Indonesian National ID

  @Column(name = "ktp_path")
  private String ktpPath;

  @Column(name = "kk_path")
  private String kkPath;

  @Column(name = "npwp_path")
  private String npwpPath;

  // Bank account information
  @Column(name = "bank_name", length = 100)
  private String bankName;

  @Column(name = "account_number", length = 30)
  private String accountNumber;

  @Column(name = "account_holder_name", length = 100)
  private String accountHolderName;

  @Column(name = "selfie_path")
  private String selfiePath;

  @Column(name = "salary_slip_path")
  private String salarySlipPath;

  @Column(length = 100)
  private String job;

  @Column(name = "company_name", length = 100)
  private String companyName;

  /**
   * Check if profile is complete for loan application submission. All fields (birthdate, phone,
   * address, nik, ktp, kk, npwp, bank account, selfie, salary slip, job, company) must be filled.
   */
  public boolean isComplete() {
    return birthdate != null
        && phone != null
        && !phone.isBlank()
        && address != null
        && !address.isBlank()
        && nik != null
        && !nik.isBlank()
        && ktpPath != null
        && !ktpPath.isBlank()
        && kkPath != null
        && !kkPath.isBlank()
        && npwpPath != null
        && !npwpPath.isBlank()
        && selfiePath != null
        && !selfiePath.isBlank()
        && salarySlipPath != null
        && !salarySlipPath.isBlank()
        && job != null
        && !job.isBlank()
        && companyName != null
        && !companyName.isBlank()
        && bankName != null
        && !bankName.isBlank()
        && accountNumber != null
        && !accountNumber.isBlank()
        && accountHolderName != null
        && !accountHolderName.isBlank();
  }
}
