package com.gvn.binarbe.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

/**
 * Product entity representing loan products (Bronze, Silver, Gold, Platinum). Each product has
 * specific amount limits, tenor, and interest rates.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false)
  private Integer tenor; // in months

  @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal interestRate; // percentage per annum
}
