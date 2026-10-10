package com.example.bookingengine.engine.rules;

import java.time.LocalTime;

import com.example.bookingengine.domain.Employee;
import com.example.bookingengine.domain.RuleType;
import com.example.bookingengine.engine.AbstractRule;
import com.example.bookingengine.engine.EvaluationContext;
import com.example.bookingengine.engine.RuleParams;
import com.example.bookingengine.engine.RuleResult;
import com.example.bookingengine.engine.TimeWindows;
import com.example.bookingengine.exception.BadRequestException;

import org.springframework.stereotype.Component;

@Component
public class ShiftWindowRule extends AbstractRule {

    @Override
    public RuleType type() {
        return RuleType.SHIFT_WINDOW;
    }

    @Override
    protected RuleResult check(EvaluationContext context, RuleParams params) {
        int grace = params.getInt("graceMinutes", 30);
        Employee employee = context.employee();
        LocalTime requested = context.request().requestedTime();

        LocalTime from = employee.getShiftStart().minusMinutes(grace);
        LocalTime to = employee.getShiftEnd().plusMinutes(grace);
        String window = employee.getShiftStart() + " to " + employee.getShiftEnd() + " with " + grace + " min grace";

        if (TimeWindows.contains(from, to, requested)) {
            return RuleResult.pass(requested + " is inside shift " + window);
        }
        return RuleResult.fail("OUTSIDE_SHIFT_WINDOW", requested + " is outside shift " + window);
    }

    @Override
    public void validateParams(RuleParams params) {
        int grace = params.getInt("graceMinutes", 30);
        if (grace < 0 || grace > 180) {
            throw new BadRequestException("INVALID_RULE_PARAMS", "graceMinutes must be between 0 and 180");
        }
    }
}
