package com.example.travelplanner.itinerary.dto;

import com.example.travelplanner.itinerary.entity.ItineraryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record UpdateItineraryRequest(
        @NotBlank String title,
        @NotBlank String city,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @PositiveOrZero Integer budget,
        @NotNull ItineraryStatus status
) {
}
