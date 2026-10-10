package com.example.bookingengine.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.example.bookingengine.domain.Booking;
import com.example.bookingengine.domain.BookingStatus;
import com.example.bookingengine.domain.Direction;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByEmployeeIdAndBookingDateAndDirectionAndStatusIn(
            Long employeeId, LocalDate bookingDate, Direction direction, Collection<BookingStatus> statuses);

    long countByCostCentreIdAndBookingDateAndDirectionAndStatusIn(
            Long costCentreId, LocalDate bookingDate, Direction direction, Collection<BookingStatus> statuses);

    List<Booking> findByStatusOrderByCreatedAtAsc(BookingStatus status);
}
