package com.example.bookingengine.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for every error we throw on purpose. It carries the HTTP status and a short
 * machine-readable code, so GlobalExceptionHandler can turn it into a clean JSON body.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
