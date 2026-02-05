package com.gvn.binarbe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for updating customer profile. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

  @NotNull(message = "Birthdate is required") private LocalDate birthdate;

  @NotBlank(message = "Phone is required")
  @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must be 10-15 digits")
  private String phone;

  @NotBlank(message = "Address is required")
  @Size(max = 500, message = "Address must not exceed 500 characters")
  private String address;

  @NotBlank(message = "NIK is required")
  @Pattern(regexp = "^[0-9]{16}$", message = "NIK must be exactly 16 digits")
  private String nik;

  @NotBlank(message = "Bank name is required")
  private String bankName;

  @NotBlank(message = "Account number is required")
  @Pattern(regexp = "^[0-9]{10,20}$", message = "Account number must be 10-20 digits")
  private String accountNumber;

  @NotBlank(message = "Account holder name is required")
  private String accountHolderName;

  @NotBlank(message = "Job is required")
  private String job;

  @NotBlank(message = "Company name is required")
  private String companyName;
}
