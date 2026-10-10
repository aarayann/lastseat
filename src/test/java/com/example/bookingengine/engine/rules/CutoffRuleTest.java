package com.example.bookingengine.engine.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import com.example.bookingengine.TestData;
import com.example.bookingengine.domain.Direction;
import com.example.bookingengine.domain.Employee;
import com.example.bookingengine.domain.Gender;
import com.example.bookingengine.domain.Outcome;
import com.example.bookingengine.engine.RuleParams;
import com.example.bookingengine.engine.RuleResult;

import org.junit.jupiter.api.Test;

class CutoffRuleTest {

    private final CutoffRule rule = new CutoffRule();
    private final Employee employee = TestData.employee(Gender.MALE, "09:00", "18:00");

    private RuleResult checkAt(String now, int minutesBefore) {
        RuleParams params = new RuleParams(Map.of("minutesBefore", minutesBefore));
        return rule.evaluate(TestData.context(employee, TestData.request(Direction.LOGIN, "09:00", false), now), params);
    }

    @Test
    void exactlyAtTheCutoffPasses() {
        assertEquals(Outcome.PASS, checkAt("2026-10-12T08:00", 60).outcome());
    }

    @Test
    void oneMinuteLateFails() {
        RuleResult result = checkAt("2026-10-12T08:01", 60);
        assertEquals(Outcome.FAIL, result.outcome());
        assertEquals("PAST_CUTOFF", result.reasonCode());
    }

    @Test
    void departureInThePastFails() {
        assertEquals(Outcome.FAIL, checkAt("2026-10-12T10:00", 60).outcome());
    }

    @Test
    void changingTheParameterChangesTheAnswer() {
        assertEquals(Outcome.FAIL, checkAt("2026-10-12T08:20", 60).outcome());
        assertEquals(Outcome.PASS, checkAt("2026-10-12T08:20", 30).outcome());
    }
}
