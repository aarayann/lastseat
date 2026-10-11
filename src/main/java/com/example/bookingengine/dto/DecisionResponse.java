package com.example.bookingengine.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.bookingengine.domain.Booking;
import com.example.bookingengine.domain.BookingStatus;
import com.example.bookingengine.domain.Decision;
import com.example.bookingengine.engine.EvaluationOutcome;
import com.example.bookingengine.engine.TraceEntry;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DecisionResponse(
        Decision decision,
        Long bookingId,
        BookingStatus status,
        List<Reason> reasons,
        long configVersion,
        LocalDateTime evaluatedAt,
        List<TraceEntry> trace) {

    public record Reason(String code, String message) {
    }

    public static DecisionResponse of(EvaluationOutcome outcome, Booking booking, long configVersion,
                                      LocalDateTime evaluatedAt, boolean includeTrace) {
        List<Reason> reasons = outcome.failures().stream()
                .map(failure -> new Reason(failure.reasonCode(), failure.detail()))
                .toList();
        return new DecisionResponse(
                outcome.decision(),
                booking == null ? null : booking.getId(),
                booking == null ? null : booking.getStatus(),
                reasons,
                configVersion,
                evaluatedAt,
                includeTrace ? outcome.trace() : null);
    }
}
