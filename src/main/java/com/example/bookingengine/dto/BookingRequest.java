package com.example.bookingengine.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.example.bookingengine.domain.Direction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull Long employeeId,
        @NotNull LocalDate date,
        @NotNull Direction direction,
        @NotNull LocalTime requestedTime,
        @NotBlank String pickup,
        @NotBlank String drop,
        boolean escort,
        boolean packedLunch,
        boolean parking) {
}
