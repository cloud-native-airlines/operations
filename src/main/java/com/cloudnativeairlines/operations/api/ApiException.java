package com.cloudnativeairlines.operations.api;

import org.springframework.http.HttpStatus;

/**
 * An error that maps to the shared {@code {"error":{"code","message"}}} envelope.
 * Codes mirror the ADS-B service: invalid_request (400), not_found (404),
 * conflict (409), database_error (500).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public static ApiException invalidRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "invalid_request", message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "not_found", message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "conflict", message);
    }
}
