package com.gvn.binarbe.service;

import com.gvn.binarbe.entity.User;
import com.gvn.binarbe.enums.LoanStatus;

/** Service interface for sending push notifications via Firebase Cloud Messaging. */
public interface PushNotificationService {

  /**
   * Send push notification to customer when loan status changes.
   *
   * @param customer the customer to notify
   * @param loanId the loan application ID
   * @param newStatus the new loan status
   * @param note optional note from approver
   */
  void sendLoanStatusNotification(User customer, Long loanId, LoanStatus newStatus, String note);
}
