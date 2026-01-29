package com.gvn.binarbe.service;

import com.gvn.binarbe.dto.request.ProductRequest;
import com.gvn.binarbe.dto.response.ProductResponse;
import java.util.List;

public interface ProductService {
  ProductResponse create(ProductRequest request);

  List<ProductResponse> getAll();

  ProductResponse getById(Long id);

  ProductResponse update(Long id, ProductRequest request);

  void delete(Long id);
}
