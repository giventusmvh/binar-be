package com.gvn.binarbe.util;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Utility class for creating consistent ResponseEntity responses. */
public final class ResponseUtil {

  private ResponseUtil() {
    // Private constructor to prevent instantiation
  }

  /** Create a success response with data (HTTP 200). */
  public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
    return ResponseEntity.ok(ApiResponse.success(data));
  }

  /** Create a success response with message and data (HTTP 200). */
  public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
    return ResponseEntity.ok(ApiResponse.success(message, data));
  }

  /** Create a success response with only message (HTTP 200). */
  public static <T> ResponseEntity<ApiResponse<T>> ok(String message) {
    return ResponseEntity.ok(ApiResponse.success(message));
  }

  /** Create a created response with data (HTTP 201). */
  public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Created successfully", data));
  }

  /** Create a created response with message and data (HTTP 201). */
  public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(message, data));
  }

  /** Create a bad request response (HTTP 400). */
  public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
  }

  /** Create a bad request response with errors (HTTP 400). */
  public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message, List<String> errors) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message, errors));
  }

  /** Create an unauthorized response (HTTP 401). */
  public static <T> ResponseEntity<ApiResponse<T>> unauthorized(String message) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
  }

  /** Create a forbidden response (HTTP 403). */
  public static <T> ResponseEntity<ApiResponse<T>> forbidden(String message) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
  }

  /** Create a not found response (HTTP 404). */
  public static <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
  }

  /** Create an internal server error response (HTTP 500). */
  public static <T> ResponseEntity<ApiResponse<T>> serverError(String message) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(message));
  }
}
