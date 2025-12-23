package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.dto.request.SelectPlafondRequest;
import com.gvn.binarbe.dto.response.ProductResponse;
import com.gvn.binarbe.dto.response.UserPlafondResponse;
import com.gvn.binarbe.entity.Product;
import com.gvn.binarbe.entity.User;
import com.gvn.binarbe.entity.UserPlafond;
import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.repository.ProductRepository;
import com.gvn.binarbe.repository.UserPlafondRepository;
import com.gvn.binarbe.repository.UserRepository;
import com.gvn.binarbe.service.PlafondService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of PlafondService for credit limit operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlafondServiceImpl implements PlafondService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final UserPlafondRepository userPlafondRepository;

    @Override
    @Transactional
    public UserPlafondResponse selectPlafond(String email, SelectPlafondRequest request) {
        log.info("Selecting plafond for user: {}", email);

        // Get user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        // Check if user already has an active plafond
        if (userPlafondRepository.existsByUserIdAndIsActiveTrue(user.getId())) {
            throw BusinessException.badRequest(
                    "You already have an active plafond. Cannot select another one.");
        }

        // Get product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> BusinessException.notFound("Product not found"));

        // Create user plafond
        UserPlafond userPlafond = UserPlafond.builder()
                .user(user)
                .product(product)
                .isActive(true)
                .build();

        userPlafond = userPlafondRepository.save(userPlafond);

        log.info("Plafond selected: User={}, Product={}", email, product.getName());

        return mapToResponse(userPlafond);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPlafondResponse getMyPlafond(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        UserPlafond userPlafond = userPlafondRepository.findByUserIdWithProduct(user.getId())
                .orElseThrow(() -> BusinessException.notFound(
                        "You don't have an active plafond. Please select a plafond first."));

        return mapToResponse(userPlafond);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActivePlafond(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        return userPlafondRepository.existsByUserIdAndIsActiveTrue(user.getId());
    }

    private UserPlafondResponse mapToResponse(UserPlafond userPlafond) {
        return UserPlafondResponse.builder()
                .id(userPlafond.getId())
                .product(mapToProductResponse(userPlafond.getProduct()))
                .assignedAt(userPlafond.getAssignedAt())
                .isActive(userPlafond.getIsActive())
                .build();
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .amount(product.getAmount())
                .tenor(product.getTenor())
                .interestRate(product.getInterestRate())
                .build();
    }
}
