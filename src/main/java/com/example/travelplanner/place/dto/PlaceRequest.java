package com.example.travelplanner.place.dto;

import com.example.travelplanner.place.entity.PlaceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlaceRequest(
        @NotBlank String name,
        @NotNull PlaceCategory category,
        @NotBlank String region,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String priceRange,
        String openHours
) {
}
