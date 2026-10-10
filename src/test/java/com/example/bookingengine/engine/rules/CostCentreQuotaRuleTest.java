package com.example.bookingengine.engine.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.example.bookingengine.TestData;
import com.example.bookingengine.domain.Direction;
import com.example.bookingengine.domain.Gender;
import com.example.bookingengine.domain.Outcome;
import com.example.bookingengine.engine.EvaluationContext;
import com.example.bookingengine.engine.RuleParams;
import com.example.bookingengine.engine.RuleResult;
import com.example.bookingengine.repository.BookingRepository;

import org.junit.jupiter.api.Test;

class CostCentreQuotaRuleTest {

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final CostCentreQuotaRule rule = new CostCentreQuotaRule(bookings);
    private final RuleParams twentySeats = new RuleParams(Map.of("seatsPerDay", 20));
    private final EvaluationContext context = TestData.context(
            TestData.employee(Gender.MALE, "09:00", "18:00"),
            TestData.request(Direction.LOGIN, "09:00", false),
            "2026-10-12T07:00");

    private void seatsAlreadyUsed(long used) {
        when(bookings.countByCostCentreIdAndBookingDateAndDirectionAndStatusIn(any(), any(), any(), any()))
                .thenReturn(used);
    }

    @Test
    void passesWhenSeatsAreLeft() {
        seatsAlreadyUsed(2);
        assertEquals(Outcome.PASS, rule.evaluate(context, twentySeats).outcome());
    }

    @Test
    void passesForTheLastSeat() {
        seatsAlreadyUsed(19);
        assertEquals(Outcome.PASS, rule.evaluate(context, twentySeats).outcome());
    }

    @Test
    void failsWhenAllSeatsAreTaken() {
        seatsAlreadyUsed(20);
        RuleResult result = rule.evaluate(context, twentySeats);
        assertEquals(Outcome.FAIL, result.outcome());
        assertEquals("QUOTA_EXCEEDED", result.reasonCode());
    }
}
