package com.example.travelplanner.place.dto;

import com.example.travelplanner.place.entity.Place;

import java.math.BigDecimal;
import java.time.LocalTime;

public record PlaceResponse(
        Long id,
        String name,
        String category,
        String region,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String priceRange,
        String openHours,
        LocalTime openingTime,
        LocalTime closingTime,
        Integer estimatedCost,
        Integer recommendedVisitMinutes
) {
    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
                place.getId(),
                place.getName(),
                place.getCategory().name(),
                place.getRegion(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getPriceRange(),
                place.getOpenHours(),
                place.getOpeningTime(),
                place.getClosingTime(),
                place.getEstimatedCost(),
                place.getRecommendedVisitMinutes()
        );
    }
}
