package com.gvn.binarbe.dto.response;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for user profile details. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

  private Long id;
  private LocalDate birthdate;
  private String phone;
  private String address;
  private String nik;
  private Boolean isComplete;
  private String ktpUrl;
  private String kkUrl;
  private String npwpUrl;
}
