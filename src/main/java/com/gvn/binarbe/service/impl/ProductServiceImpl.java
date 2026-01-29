package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.dto.request.ProductRequest;
import com.gvn.binarbe.dto.response.ProductResponse;
import com.gvn.binarbe.entity.Product;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.repository.ProductRepository;
import com.gvn.binarbe.service.ProductService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;

  @Override
  @Transactional
  public ProductResponse create(ProductRequest request) {
    if (productRepository.existsByName(request.getName())) {
      throw BusinessException.badRequest(
          "Product with name " + request.getName() + " already exists");
    }

    Product product =
        Product.builder()
            .name(request.getName())
            .amount(request.getAmount())
            .tenor(request.getTenor())
            .interestRate(request.getInterestRate())
            .build();

    product = productRepository.save(product);
    return mapToResponse(product);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProductResponse> getAll() {
    return productRepository.findAll().stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public ProductResponse getById(Long id) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> BusinessException.notFound("Product not found with id: " + id));
    return mapToResponse(product);
  }

  @Override
  @Transactional
  public ProductResponse update(Long id, ProductRequest request) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> BusinessException.notFound("Product not found with id: " + id));

    // Check name uniqueness only if name is changed
    if (!product.getName().equals(request.getName())
        && productRepository.existsByName(request.getName())) {
      throw BusinessException.badRequest(
          "Product with name " + request.getName() + " already exists");
    }

    product.setName(request.getName());
    product.setAmount(request.getAmount());
    product.setTenor(request.getTenor());
    product.setInterestRate(request.getInterestRate());

    product = productRepository.save(product);
    return mapToResponse(product);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    if (!productRepository.existsById(id)) {
      throw BusinessException.notFound("Product not found with id: " + id);
    }
    productRepository.deleteById(id);
  }

  private ProductResponse mapToResponse(Product product) {
    return ProductResponse.builder()
        .id(product.getId())
        .name(product.getName())
        .amount(product.getAmount())
        .tenor(product.getTenor())
        .interestRate(product.getInterestRate())
        .build();
  }
}
