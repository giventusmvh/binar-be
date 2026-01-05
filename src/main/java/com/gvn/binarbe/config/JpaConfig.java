package com.gvn.binarbe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** JPA configuration class. Enables JPA auditing for automatic timestamp management. */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
  // Configuration is handled by annotations
}
