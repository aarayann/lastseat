package com.example.bookingengine.dto;

import java.util.Map;

import com.example.bookingengine.domain.Decision;

public record RuleConfigUpdate(
        Boolean enabled,
        Integer priority,
        Decision onFail,
        Map<String, Object> params) {
}
