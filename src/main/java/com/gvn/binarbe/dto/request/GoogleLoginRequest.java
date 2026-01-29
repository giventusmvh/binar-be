package com.gvn.binarbe.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Google login. The idToken is obtained from Google Sign-In on the Android client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {

  /** Google ID token from Android Google Sign-In SDK */
  @NotBlank(message = "ID token is required")
  private String idToken;

  /** Optional FCM token for push notifications */
  private String fcmToken;
}
