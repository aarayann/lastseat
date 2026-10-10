package com.example.bookingengine.observability;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request an id, puts it on every log line (through MDC) and sends it back
 * in the X-Request-Id header, so one failing request can be found in the logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // runs before security, so even 401s get an id
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Request-Id";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = request.getHeader(HEADER);
        if (requestId == null || !SAFE_ID.matcher(requestId).matches()) {
            requestId = UUID.randomUUID().toString().substring(0, 8); // never trust odd input in logs
        }
        MDC.put("requestId", requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId"); // threads are reused, so always clean up
        }
    }
}
