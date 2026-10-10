package com.example.bookingengine.engine;

import com.example.bookingengine.domain.Decision;
import com.example.bookingengine.domain.RuleType;

public record ActiveRule(Long id, RuleType type, int priority, Decision onFail, RuleParams params) {
}
