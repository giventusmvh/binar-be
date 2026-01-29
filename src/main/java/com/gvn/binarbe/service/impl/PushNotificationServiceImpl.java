package com.gvn.binarbe.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.gvn.binarbe.entity.User;
import com.gvn.binarbe.enums.LoanStatus;
import com.gvn.binarbe.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Implementation of PushNotificationService using Firebase Cloud Messaging. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {

  private final FirebaseMessaging firebaseMessaging;

  @Override
  public void sendLoanStatusNotification(
      User customer, Long loanId, LoanStatus newStatus, String note) {
    if (firebaseMessaging == null) {
      log.warn("Firebase not initialized, skipping push notification");
      return;
    }

    String fcmToken = customer.getFcmToken();
    if (fcmToken == null || fcmToken.isEmpty()) {
      log.info("No FCM token for customer {}, skipping push notification", customer.getEmail());
      return;
    }

    String title = getNotificationTitle(newStatus);
    String body = getNotificationBody(newStatus, note);

    try {
      Message message =
          Message.builder()
              .setToken(fcmToken)
              .setNotification(Notification.builder().setTitle(title).setBody(body).build())
              .putData("loanId", loanId.toString())
              .putData("status", newStatus.name())
              .putData("click_action", "LOAN_DETAIL")
              .build();

      String response = firebaseMessaging.send(message);
      log.info(
          "Push notification sent to customer {} for loan {}: {}",
          customer.getEmail(),
          loanId,
          response);
    } catch (FirebaseMessagingException e) {
      log.error(
          "Failed to send push notification to customer {}: {}",
          customer.getEmail(),
          e.getMessage());
    }
  }

  private String getNotificationTitle(LoanStatus status) {
    return switch (status) {
      case SUBMITTED -> "Pengajuan Berhasil Dikirim 📝";
      case MARKETING_APPROVED -> "Pengajuan Sedang Diproses";
      case BRANCH_MANAGER_APPROVED -> "Pengajuan Hampir Selesai";
      case DISBURSED -> "Pengajuan Disetujui ✅";
      case MARKETING_REJECTED, BRANCH_MANAGER_REJECTED, REJECTED -> "Pengajuan Ditolak ❌";
      default -> "Status Pengajuan Diperbarui";
    };
  }

  private String getNotificationBody(LoanStatus status, String note) {
    return switch (status) {
      case SUBMITTED ->
          "Pengajuan pinjaman Anda telah diterima dan akan segera direview oleh Marketing.";
      case MARKETING_APPROVED -> "Pengajuan pinjaman Anda sedang dalam review oleh Branch Manager.";
      case BRANCH_MANAGER_APPROVED ->
          "Pengajuan pinjaman Anda menunggu persetujuan akhir dari Backoffice.";
      case DISBURSED ->
          "Selamat! Pengajuan pinjaman Anda telah disetujui dan akan segera dicairkan.";
      case MARKETING_REJECTED, BRANCH_MANAGER_REJECTED, REJECTED -> {
        String reason = (note != null && !note.isEmpty()) ? note : "Tidak memenuhi kriteria";
        yield "Maaf, pengajuan pinjaman Anda ditolak. Alasan: " + reason;
      }
      default -> "Status pengajuan pinjaman Anda telah diperbarui menjadi " + status.name();
    };
  }
}
