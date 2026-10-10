package com.example.bookingengine.engine;

import java.time.LocalTime;

public final class TimeWindows {

    private TimeWindows() {
    }

    public static boolean contains(LocalTime start, LocalTime end, LocalTime time) {
        if (!start.isAfter(end)) {
            return !time.isBefore(start) && !time.isAfter(end);
        }
        return !time.isBefore(start) || !time.isAfter(end);
    }
}
