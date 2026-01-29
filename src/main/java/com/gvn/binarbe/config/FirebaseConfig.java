package com.gvn.binarbe.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration class for Firebase Admin SDK. Initializes Firebase for push notifications. */
@Slf4j
@Configuration
public class FirebaseConfig {

  @Value("${firebase.credentials.path:src/main/resources/firebase-service-account.json}")
  private String firebaseCredentialsPath;

  @PostConstruct
  public void initialize() {
    try {
      if (FirebaseApp.getApps().isEmpty()) {
        FileInputStream serviceAccount = new FileInputStream(firebaseCredentialsPath);

        FirebaseOptions options =
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);
        log.info("Firebase initialized successfully");
      }
    } catch (IOException e) {
      log.error("Failed to initialize Firebase: {}", e.getMessage());
      log.warn("Push notifications will not be available");
    }
  }

  @Bean
  public FirebaseMessaging firebaseMessaging() {
    if (FirebaseApp.getApps().isEmpty()) {
      log.warn("FirebaseApp not initialized, returning null for FirebaseMessaging");
      return null;
    }
    return FirebaseMessaging.getInstance();
  }
}
