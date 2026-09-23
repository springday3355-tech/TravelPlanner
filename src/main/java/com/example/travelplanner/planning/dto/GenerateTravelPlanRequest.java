package com.example.travelplanner.planning.dto;

import com.example.travelplanner.place.entity.PlaceCategory;
import com.example.travelplanner.planning.entity.CompanionType;
import com.example.travelplanner.planning.entity.TravelPace;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record GenerateTravelPlanRequest(
        @NotBlank String title,
        @NotBlank String city,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull LocalDate endDate,
        @PositiveOrZero Integer budget,
        Set<PlaceCategory> preferredCategories,
        @NotNull CompanionType companionType,
        @NotNull TravelPace travelPace,
        LocalTime dailyStartTime,
        LocalTime dailyEndTime,
        Set<@Positive Long> requiredPlaceIds,
        Set<@Positive Long> excludedPlaceIds
) {
}
