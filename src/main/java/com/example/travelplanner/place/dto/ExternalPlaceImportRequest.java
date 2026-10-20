package com.example.travelplanner.place.dto;

import com.example.travelplanner.place.entity.PlaceCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record ExternalPlaceImportRequest(
        @NotBlank String query,
        @NotBlank String region,
        Set<PlaceCategory> categories,
        @Min(1) @Max(20) Integer limit
) {
}
