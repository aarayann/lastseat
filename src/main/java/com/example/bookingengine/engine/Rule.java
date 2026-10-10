package com.example.bookingengine.engine;

import com.example.bookingengine.domain.RuleType;

public interface Rule {

    RuleType type();

    RuleResult evaluate(EvaluationContext context, RuleParams params);

    default void validateParams(RuleParams params) {
    }

    default boolean needsDatabase() {
        return false;
    }
}
