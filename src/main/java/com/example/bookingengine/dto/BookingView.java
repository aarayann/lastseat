package com.example.bookingengine.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.example.bookingengine.domain.Booking;
import com.example.bookingengine.domain.BookingStatus;
import com.example.bookingengine.domain.Direction;

public record BookingView(
        Long id,
        Long employeeId,
        LocalDate date,
        Direction direction,
        LocalTime requestedTime,
        String pickup,
        String drop,
        boolean escort,
        BookingStatus status,
        String reasons,
        long configVersion) {

    public static BookingView from(Booking booking) {
        return new BookingView(
                booking.getId(),
                booking.getEmployeeId(),
                booking.getBookingDate(),
                booking.getDirection(),
                booking.getRequestedTime(),
                booking.getPickup(),
                booking.getDropLocation(),
                booking.isEscort(),
                booking.getStatus(),
                booking.getReasons(),
                booking.getConfigVersion());
    }
}
