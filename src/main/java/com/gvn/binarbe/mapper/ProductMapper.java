package com.gvn.binarbe.mapper;

import com.gvn.binarbe.dto.response.ProductResponse;
import com.gvn.binarbe.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

  public ProductResponse toResponse(Product product) {
    if (product == null) return null;
    return ProductResponse.builder()
        .id(product.getId())
        .name(product.getName())
        .amount(product.getAmount())
        .tenor(product.getTenor())
        .interestRate(product.getInterestRate())
        .build();
  }
}
