package com.gvn.binarbe.service;

/**
 * Service interface for email operations.
 */
public interface EmailService {

    /**
     * Send password reset email to user.
     *
     * @param toEmail    recipient email address
     * @param resetToken unique token for password reset
     */
    void sendPasswordResetEmail(String toEmail, String resetToken);
}
