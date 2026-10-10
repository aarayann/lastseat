package com.example.bookingengine.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class TimeWindowsTest {

    private static LocalTime t(String time) {
        return LocalTime.parse(time);
    }

    @Test
    void normalWindowIncludesBothEdges() {
        assertTrue(TimeWindows.contains(t("09:00"), t("18:00"), t("09:00")));
        assertTrue(TimeWindows.contains(t("09:00"), t("18:00"), t("18:00")));
        assertFalse(TimeWindows.contains(t("09:00"), t("18:00"), t("08:59")));
        assertFalse(TimeWindows.contains(t("09:00"), t("18:00"), t("18:01")));
    }

    @Test
    void windowAcrossMidnight() {
        assertTrue(TimeWindows.contains(t("22:00"), t("06:00"), t("23:30")));
        assertTrue(TimeWindows.contains(t("22:00"), t("06:00"), t("00:00")));
        assertTrue(TimeWindows.contains(t("22:00"), t("06:00"), t("05:30")));
        assertFalse(TimeWindows.contains(t("22:00"), t("06:00"), t("12:00")));
        assertFalse(TimeWindows.contains(t("22:00"), t("06:00"), t("21:59")));
        assertFalse(TimeWindows.contains(t("22:00"), t("06:00"), t("06:01")));
    }
}
