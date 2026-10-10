package com.example.bookingengine.engine;

import com.example.bookingengine.domain.Decision;
import com.example.bookingengine.domain.Outcome;
import com.example.bookingengine.domain.RuleType;

public record TraceEntry(int order, RuleType rule, Decision onFail, Outcome outcome, String reasonCode, String detail) {
}
