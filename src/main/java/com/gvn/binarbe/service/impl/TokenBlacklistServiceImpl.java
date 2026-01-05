package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.service.TokenBlacklistService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Implementation of TokenBlacklistService using Redis. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

  private static final String TOKEN_BLACKLIST_KEY_PREFIX = "blacklist:";
  private static final String PASSWORD_CHANGED_KEY_PREFIX = "password-changed:";

  private final StringRedisTemplate redisTemplate;

  @Value("${jwt.expiration}")
  private long jwtExpiration;

  @Override
  public boolean isTokenBlacklisted(String token, String email, long issuedAt) {
    // Check 1: Is this specific token blacklisted?
    String blacklistKey = TOKEN_BLACKLIST_KEY_PREFIX + token;
    if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
      log.debug("Token is directly blacklisted");
      return true;
    }

    // Check 2: Was password changed after this token was issued?
    String passwordChangedKey = PASSWORD_CHANGED_KEY_PREFIX + email;
    String passwordChangedTimeStr = redisTemplate.opsForValue().get(passwordChangedKey);

    if (passwordChangedTimeStr != null) {
      long passwordChangedTime = Long.parseLong(passwordChangedTimeStr);
      if (issuedAt < passwordChangedTime) {
        log.debug("Token issued before password change, considered blacklisted");
        return true;
      }
    }

    return false;
  }

  @Override
  public void blacklistToken(String token, long ttlMillis) {
    if (ttlMillis > 0) {
      String blacklistKey = TOKEN_BLACKLIST_KEY_PREFIX + token;
      redisTemplate.opsForValue().set(blacklistKey, "1", ttlMillis, TimeUnit.MILLISECONDS);
      log.info("Token blacklisted successfully");
    }
  }

  @Override
  public void invalidateAllUserTokens(String email) {
    String passwordChangedKey = PASSWORD_CHANGED_KEY_PREFIX + email;
    redisTemplate
        .opsForValue()
        .set(
            passwordChangedKey,
            String.valueOf(System.currentTimeMillis()),
            jwtExpiration,
            TimeUnit.MILLISECONDS);
    log.info("All tokens invalidated for user: {}", email);
  }
}
