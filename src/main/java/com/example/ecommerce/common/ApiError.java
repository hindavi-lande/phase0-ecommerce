package com.example.ecommerce.common;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform error body returned for every handled failure.
 * {@code fieldErrors} is only populated for validation failures.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, Map.of());
    }

    public static ApiError validation(int status, String error, String message, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, fieldErrors);
    }
}
