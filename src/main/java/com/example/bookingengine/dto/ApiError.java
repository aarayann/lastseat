package com.example.bookingengine.dto;

import java.time.Instant;

/** The one shape every error response has. No stack traces, no SQL. */
public record ApiError(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        String requestId) {
}
