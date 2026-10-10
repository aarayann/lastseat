package com.example.bookingengine.engine;

import com.example.bookingengine.domain.Outcome;

public record RuleResult(Outcome outcome, String reasonCode, String detail) {

    public static RuleResult pass(String detail) {
        return new RuleResult(Outcome.PASS, null, detail);
    }

    public static RuleResult fail(String reasonCode, String detail) {
        return new RuleResult(Outcome.FAIL, reasonCode, detail);
    }

    public boolean failed() {
        return outcome == Outcome.FAIL;
    }
}
