package com.example.bookingengine.dto;

import java.util.Map;

import com.example.bookingengine.domain.Decision;
import com.example.bookingengine.domain.RuleType;

public record RuleConfigView(
        Long id,
        String clientId,
        Long costCentreId,
        RuleType ruleType,
        boolean enabled,
        int priority,
        Decision onFail,
        Map<String, Object> params) {
}
