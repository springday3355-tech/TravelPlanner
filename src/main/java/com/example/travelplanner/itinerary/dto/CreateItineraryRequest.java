package com.example.travelplanner.itinerary.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record CreateItineraryRequest(
        @NotBlank String title,
        @NotBlank String city,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull LocalDate endDate,
        @PositiveOrZero Integer budget
) {
}
