package com.example.travelplanner.itinerary.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record AddDayRequest(
        @NotNull @Positive Integer dayNumber,
        @NotNull LocalDate date
) {
}
