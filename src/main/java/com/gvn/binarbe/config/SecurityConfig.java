package com.gvn.binarbe.config;

import com.gvn.binarbe.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Security configuration for JWT-based authentication and authorization. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthFilter;
  private final UserDetailsService userDetailsService;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/branches/**")
                .permitAll()
                .requestMatchers("/error")
                .permitAll()
                .requestMatchers("/actuator/**")
                .permitAll()

                // Admin endpoints - superadmin only (kept for safety)
                .requestMatchers("/api/admin/**")
                .hasRole("SUPERADMIN")
                .requestMatchers("/api/admin/branches/**")
                .hasRole("SUPERADMIN")
                .requestMatchers("/api/users/**")
                .hasAnyRole("SUPERADMIN", "MARKETING", "BRANCH_MANAGER", "BACKOFFICE")

                // All other requests require authentication
                // Permission-based access control is handled by @PreAuthorize
                .anyRequest()
                .authenticated())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
    org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
    // Production: Only allow specific origins
    // Note: Mobile apps (Android/iOS) don't need CORS - they bypass browser
    // restrictions
    configuration.setAllowedOriginPatterns(
        java.util.List.of(
            "https://ehefin-fe.vercel.app", // Production frontend
            "https://*.vercel.app", // Vercel preview deployments
            "http://localhost:*", // Local development
            "http://127.0.0.1:*" // Local development alternative
        ));
    configuration.setAllowedMethods(
        java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(java.util.List.of("*"));
    configuration.setAllowCredentials(true);
    org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
    AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
    authBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    return authBuilder.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
