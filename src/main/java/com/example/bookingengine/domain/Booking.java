package com.example.bookingengine.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.example.bookingengine.dto.BookingRequest;
import com.example.bookingengine.exception.ConflictException;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(indexes = {
        @Index(name = "idx_booking_employee_day", columnList = "employee_id, booking_date, direction"),
        @Index(name = "idx_booking_cost_centre_day", columnList = "cost_centre_id, booking_date, direction, status")
})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "cost_centre_id", nullable = false)
    private Long costCentreId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private Direction direction;

    private LocalTime requestedTime;

    private String pickup;

    private String dropLocation;

    private boolean escort;

    private boolean packedLunch;

    private boolean parking;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    private String reasons;

    private long configVersion;

    private LocalDateTime createdAt;

    protected Booking() {
    }

    public Booking(Employee employee, BookingRequest request, BookingStatus status,
                   String reasons, long configVersion, LocalDateTime createdAt) {
        this.employeeId = employee.getId();
        this.costCentreId = employee.getCostCentreId();
        this.bookingDate = request.date();
        this.direction = request.direction();
        this.requestedTime = request.requestedTime();
        this.pickup = request.pickup();
        this.dropLocation = request.drop();
        this.escort = request.escort();
        this.packedLunch = request.packedLunch();
        this.parking = request.parking();
        this.status = status;
        this.reasons = reasons;
        this.configVersion = configVersion;
        this.createdAt = createdAt;
    }

    public boolean cancel() {
        if (!status.isActive()) {
            return false;
        }
        status = BookingStatus.CANCELLED;
        return true;
    }

    public void approve() {
        requirePending();
        status = BookingStatus.CONFIRMED;
    }

    public void reject() {
        requirePending();
        status = BookingStatus.REJECTED;
    }

    private void requirePending() {
        if (status != BookingStatus.PENDING_APPROVAL) {
            throw new ConflictException("Booking " + id + " is " + status
                    + "; only PENDING_APPROVAL bookings can be approved or rejected");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getCostCentreId() {
        return costCentreId;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public Direction getDirection() {
        return direction;
    }

    public LocalTime getRequestedTime() {
        return requestedTime;
    }

    public String getPickup() {
        return pickup;
    }

    public String getDropLocation() {
        return dropLocation;
    }

    public boolean isEscort() {
        return escort;
    }

    public boolean isPackedLunch() {
        return packedLunch;
    }

    public boolean isParking() {
        return parking;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public String getReasons() {
        return reasons;
    }

    public long getConfigVersion() {
        return configVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
