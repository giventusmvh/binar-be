package com.gvn.binarbe.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Entity representing a document uploaded by a user (e.g., KTP, KK, NPWP). */
@Entity
@Table(name = "user_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDocument {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "file_path", nullable = false)
  private String filePath;

  @Column(name = "file_type")
  private String fileType;

  @Column(name = "created_at")
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();
}
