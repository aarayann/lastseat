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

class NightSafetyRuleTest {

    private final NightSafetyRule rule = new NightSafetyRule();
    private final RuleParams night = new RuleParams(Map.of("nightStart", "20:00", "nightEnd", "06:00"));
    private final Employee woman = TestData.employee(Gender.FEMALE, "14:00", "23:00");
    private final Employee man = TestData.employee(Gender.MALE, "14:00", "23:00");

    private RuleResult check(Employee employee, String time, boolean escort) {
        return rule.evaluate(TestData.context(employee, TestData.request(Direction.LOGOUT, time, escort), "2026-10-12T12:00"), night);
    }

    @Test
    void womanAtNightWithoutEscortFails() {
        RuleResult result = check(woman, "23:30", false);
        assertEquals(Outcome.FAIL, result.outcome());
        assertEquals("NIGHT_SAFETY", result.reasonCode());
    }

    @Test
    void womanAtNightWithEscortPasses() {
        assertEquals(Outcome.PASS, check(woman, "23:30", true).outcome());
    }

    @Test
    void manAtNightPasses() {
        assertEquals(Outcome.PASS, check(man, "23:30", false).outcome());
    }

    @Test
    void nightStartsExactlyAt2000() {
        assertEquals(Outcome.FAIL, check(woman, "20:00", false).outcome());
        assertEquals(Outcome.PASS, check(woman, "19:59", false).outcome());
    }
}
