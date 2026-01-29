package com.gvn.binarbe.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for registering FCM token from mobile app. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterFcmTokenRequest {

  @NotBlank(message = "FCM token is required")
  private String fcmToken;
}
