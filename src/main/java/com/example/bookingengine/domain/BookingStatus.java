package com.example.bookingengine.domain;

import java.util.List;

public enum BookingStatus {
    CONFIRMED,
    PENDING_APPROVAL,
    REJECTED,
    CANCELLED;

    /** Statuses that hold a seat and count as an existing booking. */
    public static final List<BookingStatus> ACTIVE = List.of(CONFIRMED, PENDING_APPROVAL);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
