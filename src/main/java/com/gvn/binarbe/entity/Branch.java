package com.gvn.binarbe.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

/**
 * Branch entity representing physical branch locations. Internal users belong to a specific branch.
 * Loan applications are processed at specific branches.
 */
@Entity
@Table(name = "branch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String location;

  @OneToMany(mappedBy = "branch")
  @Builder.Default
  private Set<User> users = new HashSet<>();

  @OneToMany(mappedBy = "branch")
  @Builder.Default
  private Set<LoanApplication> loanApplications = new HashSet<>();
}
