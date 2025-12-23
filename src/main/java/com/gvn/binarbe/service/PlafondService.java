package com.gvn.binarbe.service;

import com.gvn.binarbe.dto.request.SelectPlafondRequest;
import com.gvn.binarbe.dto.response.UserPlafondResponse;

/**
 * Service interface for plafond/credit limit operations.
 */
public interface PlafondService {

    /**
     * Select a product as user's plafond/credit limit.
     *
     * @param email   User's email
     * @param request Contains productId to select
     * @return UserPlafondResponse with selected plafond details
     */
    UserPlafondResponse selectPlafond(String email, SelectPlafondRequest request);

    /**
     * Get user's active plafond.
     *
     * @param email User's email
     * @return UserPlafondResponse if plafond exists, throws exception otherwise
     */
    UserPlafondResponse getMyPlafond(String email);

    /**
     * Check if user has an active plafond.
     *
     * @param email User's email
     * @return true if user has active plafond
     */
    boolean hasActivePlafond(String email);
}
