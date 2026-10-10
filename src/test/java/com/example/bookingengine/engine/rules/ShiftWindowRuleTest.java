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

class ShiftWindowRuleTest {

    private final ShiftWindowRule rule = new ShiftWindowRule();
    private final RuleParams grace30 = new RuleParams(Map.of("graceMinutes", 30));
    private final Employee dayShift = TestData.employee(Gender.MALE, "09:00", "18:00");
    private final Employee nightShift = TestData.employee(Gender.MALE, "22:00", "06:00");

    private RuleResult check(Employee employee, Direction direction, String time) {
        return rule.evaluate(TestData.context(employee, TestData.request(direction, time, false), "2026-10-10T07:00"), grace30);
    }

    @Test
    void loginAtShiftStartPasses() {
        assertEquals(Outcome.PASS, check(dayShift, Direction.LOGIN, "09:00").outcome());
    }

    @Test
    void exactlyOnTheGraceEdgePasses() {
        assertEquals(Outcome.PASS, check(dayShift, Direction.LOGIN, "08:30").outcome());
    }

    @Test
    void oneMinuteOutsideTheGraceFails() {
        RuleResult result = check(dayShift, Direction.LOGIN, "08:29");
        assertEquals(Outcome.FAIL, result.outcome());
        assertEquals("OUTSIDE_SHIFT_WINDOW", result.reasonCode());
    }

    @Test
    void nightShiftAcrossMidnightWorks() {
        assertEquals(Outcome.PASS, check(nightShift, Direction.LOGOUT, "06:15").outcome());
        assertEquals(Outcome.PASS, check(nightShift, Direction.LOGIN, "21:45").outcome());
        assertEquals(Outcome.FAIL, check(nightShift, Direction.LOGIN, "12:00").outcome());
    }
}
