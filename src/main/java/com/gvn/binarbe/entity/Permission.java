package com.gvn.binarbe.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

/**
 * Permission entity for granular access control. Permissions are assigned to roles, not directly to
 * users.
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column private String description;

  @ManyToMany(mappedBy = "permissions")
  @Builder.Default
  private Set<Role> roles = new HashSet<>();
}
