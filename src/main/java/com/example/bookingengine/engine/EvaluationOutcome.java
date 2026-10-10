package com.example.bookingengine.engine;

import java.util.List;

import com.example.bookingengine.domain.Decision;
import com.example.bookingengine.domain.Outcome;

public record EvaluationOutcome(Decision decision, List<TraceEntry> trace) {

    public List<TraceEntry> failures() {
        return trace.stream()
                .filter(entry -> entry.outcome() == Outcome.FAIL)
                .toList();
    }
}
