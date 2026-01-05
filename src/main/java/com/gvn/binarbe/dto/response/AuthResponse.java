package com.gvn.binarbe.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for successful authentication. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

  private String token;
  private String tokenType;
  private Long userId;
  private String email;
  private String name;
  private List<String> roles;
  private List<String> permissions;
}
