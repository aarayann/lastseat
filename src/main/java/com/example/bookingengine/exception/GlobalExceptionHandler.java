package com.example.bookingengine.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import com.example.bookingengine.dto.ApiError;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Turns every exception into the same ApiError JSON shape. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Our own errors: not found, forbidden, bad rule params, wrong state. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException e, HttpServletRequest request) {
        return build(e.getStatus(), e.getErrorCode(), e.getMessage(), request);
    }

    /** @Valid failed, e.g. direction missing. Lists every bad field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request);
    }

    /** Body is not valid JSON, or a value has the wrong format ("25:00", "SIDEWAYS"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Request body is not valid JSON or has a wrong value (check dates, times and enum names)", request);
    }

    /** A path variable of the wrong type, e.g. /api/bookings/abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", e.getName() + " has the wrong type", request);
    }

    /** Anything else. Spring's own errors keep their status; real bugs become a 500 without details. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception e, HttpServletRequest request) {
        if (e instanceof ErrorResponse springError) { // 404 unknown path, 405 wrong method, ...
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return build(status, status.name(), springError.getBody().getDetail(), request);
        }
        log.error("Unexpected error on {}", request.getRequestURI(), e); // full stack trace goes to the log only
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Something went wrong. Quote the requestId when you report this.", request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String errorCode, String message,
                                           HttpServletRequest request) {
        ApiError body = new ApiError(Instant.now(), status.value(), errorCode, message,
                request.getRequestURI(), MDC.get("requestId"));
        return ResponseEntity.status(status).body(body);
    }
}
