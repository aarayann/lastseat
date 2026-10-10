package com.example.bookingengine.engine.rules;

import com.example.bookingengine.domain.BookingStatus;
import com.example.bookingengine.domain.RuleType;
import com.example.bookingengine.dto.BookingRequest;
import com.example.bookingengine.engine.AbstractRule;
import com.example.bookingengine.engine.EvaluationContext;
import com.example.bookingengine.engine.RuleParams;
import com.example.bookingengine.engine.RuleResult;
import com.example.bookingengine.repository.BookingRepository;

import org.springframework.stereotype.Component;

@Component
public class DuplicateBookingRule extends AbstractRule {

    private final BookingRepository bookings;

    public DuplicateBookingRule(BookingRepository bookings) {
        this.bookings = bookings;
    }

    @Override
    public RuleType type() {
        return RuleType.DUPLICATE;
    }

    @Override
    public boolean needsDatabase() {
        return true;
    }

    @Override
    protected RuleResult check(EvaluationContext context, RuleParams params) {
        BookingRequest request = context.request();
        boolean alreadyBooked = bookings.existsByEmployeeIdAndBookingDateAndDirectionAndStatusIn(
                context.employee().getId(), request.date(), request.direction(), BookingStatus.ACTIVE);

        if (alreadyBooked) {
            return RuleResult.fail("DUPLICATE", "an active " + request.direction() + " booking already exists for "
                    + request.date());
        }
        return RuleResult.pass("no active " + request.direction() + " booking on " + request.date());
    }
}
