package com.example.travelplanner.place.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record OpeningHourRequest(
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime openingTime,
        @NotNull LocalTime closingTime,
        boolean closesNextDay
) {
}
