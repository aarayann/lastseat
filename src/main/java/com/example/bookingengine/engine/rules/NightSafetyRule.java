package com.example.bookingengine.engine.rules;

import java.time.LocalTime;

import com.example.bookingengine.domain.Gender;
import com.example.bookingengine.domain.RuleType;
import com.example.bookingengine.dto.BookingRequest;
import com.example.bookingengine.engine.AbstractRule;
import com.example.bookingengine.engine.EvaluationContext;
import com.example.bookingengine.engine.RuleParams;
import com.example.bookingengine.engine.RuleResult;
import com.example.bookingengine.engine.TimeWindows;

import org.springframework.stereotype.Component;

@Component
public class NightSafetyRule extends AbstractRule {

    @Override
    public RuleType type() {
        return RuleType.NIGHT_SAFETY;
    }

    @Override
    protected RuleResult check(EvaluationContext context, RuleParams params) {
        LocalTime nightStart = params.getTime("nightStart", LocalTime.of(20, 0));
        LocalTime nightEnd = params.getTime("nightEnd", LocalTime.of(6, 0));
        BookingRequest request = context.request();
        String nightHours = nightStart + " to " + nightEnd;

        if (context.employee().getGender() != Gender.FEMALE) {
            return RuleResult.pass("not applicable: rule covers women travelling at night");
        }
        if (!TimeWindows.contains(nightStart, nightEnd, request.requestedTime())) {
            return RuleResult.pass("not applicable: " + request.requestedTime() + " is outside night hours " + nightHours);
        }
        if (request.escort()) {
            return RuleResult.pass("night booking for a woman, escort is on the booking");
        }
        return RuleResult.fail("NIGHT_SAFETY", "female employee, " + request.requestedTime()
                + " is inside night hours " + nightHours + ", no escort on the booking");
    }

    @Override
    public void validateParams(RuleParams params) {
        params.getTime("nightStart", LocalTime.of(20, 0));
        params.getTime("nightEnd", LocalTime.of(6, 0));
    }
}
