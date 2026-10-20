package com.example.travelplanner.place.provider;

import com.example.travelplanner.place.entity.PlaceCategory;

import java.math.BigDecimal;
import java.util.List;

public record ProviderPlace(
        String externalId,
        String name,
        PlaceCategory category,
        String region,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String priceRange,
        String openHours,
        Integer estimatedCost,
        Integer recommendedVisitMinutes,
        List<ProviderOpeningHour> openingHours
) {
}
