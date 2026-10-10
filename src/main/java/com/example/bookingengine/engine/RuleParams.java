package com.example.bookingengine.engine;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

import com.example.bookingengine.exception.BadRequestException;

public class RuleParams {

    private final Map<String, Object> values;

    public RuleParams(Map<String, Object> values) {
        this.values = values == null ? Map.of() : values;
    }

    public static RuleParams empty() {
        return new RuleParams(Map.of());
    }

    public int getInt(String key, int defaultValue) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            throw new BadRequestException("INVALID_RULE_PARAMS", key + " must be a whole number, got " + value);
        }
    }

    public LocalTime getTime(String key, LocalTime defaultValue) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return LocalTime.parse(value.toString());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("INVALID_RULE_PARAMS", key + " must be a time like 20:00, got " + value);
        }
    }

    public Map<String, Object> asMap() {
        return values;
    }
}
