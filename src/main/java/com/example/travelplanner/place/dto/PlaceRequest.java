package com.example.travelplanner.place.dto;

import com.example.travelplanner.place.entity.PlaceCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record PlaceRequest(
        @NotBlank String name,
        @NotNull PlaceCategory category,
        @NotBlank String region,
        String address,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        String priceRange,
        String openHours,
        LocalTime openingTime,
        LocalTime closingTime,
        @PositiveOrZero Integer estimatedCost,
        @Min(30) @Max(480) Integer recommendedVisitMinutes,
        List<@NotNull @Valid OpeningHourRequest> openingHours
) {
}
