package com.gvn.binarbe.service;

/**
 * Service for managing token blacklist.
 * Separated from AuthService to avoid circular dependency.
 */
public interface TokenBlacklistService {

    /**
     * Check if a token is blacklisted (either directly or via password change).
     *
     * @param token    JWT token to check
     * @param email    user email from token
     * @param issuedAt token issued timestamp in milliseconds
     * @return true if token is blacklisted
     */
    boolean isTokenBlacklisted(String token, String email, long issuedAt);

    /**
     * Blacklist a single token.
     *
     * @param token     JWT token to blacklist
     * @param ttlMillis time to live in milliseconds
     */
    void blacklistToken(String token, long ttlMillis);

    /**
     * Invalidate all tokens for a user by storing password change timestamp.
     *
     * @param email user email
     */
    void invalidateAllUserTokens(String email);
}
