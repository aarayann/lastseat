package com.example.bookingengine.engine.rules;

import java.time.Duration;
import java.time.LocalDateTime;

import com.example.bookingengine.domain.RuleType;
import com.example.bookingengine.engine.AbstractRule;
import com.example.bookingengine.engine.EvaluationContext;
import com.example.bookingengine.engine.RuleParams;
import com.example.bookingengine.engine.RuleResult;
import com.example.bookingengine.exception.BadRequestException;

import org.springframework.stereotype.Component;

@Component
public class CutoffRule extends AbstractRule {

    @Override
    public RuleType type() {
        return RuleType.CUTOFF;
    }

    @Override
    protected RuleResult check(EvaluationContext context, RuleParams params) {
        int minutesBefore = params.getInt("minutesBefore", 60);
        LocalDateTime departure = LocalDateTime.of(context.request().date(), context.request().requestedTime());
        long minutesLeft = Duration.between(context.now(), departure).toMinutes();

        if (minutesLeft >= minutesBefore) {
            return RuleResult.pass("departure in " + minutesLeft + " min, cut-off is " + minutesBefore + " min");
        }
        return RuleResult.fail("PAST_CUTOFF", "departure in " + minutesLeft + " min, but bookings close "
                + minutesBefore + " min before departure");
    }

    @Override
    public void validateParams(RuleParams params) {
        int minutesBefore = params.getInt("minutesBefore", 60);
        if (minutesBefore < 0 || minutesBefore > 1440) {
            throw new BadRequestException("INVALID_RULE_PARAMS", "minutesBefore must be between 0 and 1440");
        }
    }
}
