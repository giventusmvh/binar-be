package com.gvn.binarbe.controller;

import com.gvn.binarbe.dto.request.ProductRequest;
import com.gvn.binarbe.dto.response.ProductResponse;
import com.gvn.binarbe.service.ProductService;
import com.gvn.binarbe.util.ApiResponse;
import com.gvn.binarbe.util.ResponseUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  @PostMapping
  @PreAuthorize("hasRole('SUPERADMIN') and hasAuthority('PRODUCT_MANAGE')")
  public ResponseEntity<ApiResponse<ProductResponse>> create(
      @Valid @RequestBody ProductRequest request) {
    ProductResponse response = productService.create(request);
    return ResponseUtil.created("Product created successfully", response);
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<ProductResponse>>> getAll() {
    List<ProductResponse> response = productService.getAll();
    return ResponseUtil.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable Long id) {
    ProductResponse response = productService.getById(id);
    return ResponseUtil.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('SUPERADMIN') and hasAuthority('PRODUCT_MANAGE')")
  public ResponseEntity<ApiResponse<ProductResponse>> update(
      @PathVariable Long id, @Valid @RequestBody ProductRequest request) {
    ProductResponse response = productService.update(id, request);
    return ResponseUtil.ok("Product updated successfully", response);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('SUPERADMIN') and hasAuthority('PRODUCT_MANAGE')")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
    productService.delete(id);
    return ResponseUtil.ok("Product deleted successfully", null);
  }
}
