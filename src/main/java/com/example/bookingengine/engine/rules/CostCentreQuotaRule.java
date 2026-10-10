package com.example.bookingengine.engine.rules;

import com.example.bookingengine.domain.BookingStatus;
import com.example.bookingengine.domain.RuleType;
import com.example.bookingengine.dto.BookingRequest;
import com.example.bookingengine.engine.AbstractRule;
import com.example.bookingengine.engine.EvaluationContext;
import com.example.bookingengine.engine.RuleParams;
import com.example.bookingengine.engine.RuleResult;
import com.example.bookingengine.exception.BadRequestException;
import com.example.bookingengine.repository.BookingRepository;

import org.springframework.stereotype.Component;

@Component
public class CostCentreQuotaRule extends AbstractRule {

    private final BookingRepository bookings;

    public CostCentreQuotaRule(BookingRepository bookings) {
        this.bookings = bookings;
    }

    @Override
    public RuleType type() {
        return RuleType.COST_CENTRE_QUOTA;
    }

    @Override
    public boolean needsDatabase() {
        return true;
    }

    @Override
    protected RuleResult check(EvaluationContext context, RuleParams params) {
        int seats = params.getInt("seatsPerDay", 20);
        BookingRequest request = context.request();
        long used = bookings.countByCostCentreIdAndBookingDateAndDirectionAndStatusIn(
                context.employee().getCostCentreId(), request.date(), request.direction(), BookingStatus.ACTIVE);

        if (used < seats) {
            return RuleResult.pass(used + " of " + seats + " seats used");
        }
        return RuleResult.fail("QUOTA_EXCEEDED", "all " + seats + " " + request.direction()
                + " seats for this cost-centre on " + request.date() + " are taken");
    }

    @Override
    public void validateParams(RuleParams params) {
        if (params.getInt("seatsPerDay", 20) < 0) {
            throw new BadRequestException("INVALID_RULE_PARAMS", "seatsPerDay cannot be negative");
        }
    }
}
