package com.example.bookingengine.engine;

import java.time.LocalDateTime;

import com.example.bookingengine.domain.Employee;
import com.example.bookingengine.dto.BookingRequest;

public record EvaluationContext(Employee employee, BookingRequest request, LocalDateTime now) {
}
