package com.example.bookingengine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.example.bookingengine.domain.Direction;
import com.example.bookingengine.domain.Employee;
import com.example.bookingengine.domain.Gender;
import com.example.bookingengine.dto.BookingRequest;
import com.example.bookingengine.engine.EvaluationContext;

public final class TestData {

    public static final LocalDate DAY = LocalDate.of(2026, 10, 12);

    private TestData() {
    }

    public static Employee employee(Gender gender, String shiftStart, String shiftEnd) {
        return new Employee("Test Person", "test", "acme", "QA", 1L, gender,
                LocalTime.parse(shiftStart), LocalTime.parse(shiftEnd));
    }

    public static BookingRequest request(Direction direction, String time, boolean escort) {
        return new BookingRequest(1L, DAY, direction, LocalTime.parse(time), "Home", "Office",
                escort, false, false);
    }

    public static EvaluationContext context(Employee employee, BookingRequest request, String now) {
        return new EvaluationContext(employee, request, LocalDateTime.parse(now));
    }
}
