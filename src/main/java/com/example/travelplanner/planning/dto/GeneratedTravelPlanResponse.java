package com.example.travelplanner.planning.dto;

import com.example.travelplanner.itinerary.dto.ItineraryDetailResponse;

import java.util.List;

public record GeneratedTravelPlanResponse(
        String generator,
        int selectedPlaceCount,
        int availableSlotCount,
        double estimatedDistanceKm,
        int estimatedTravelMinutes,
        int estimatedCost,
        List<String> warnings,
        ItineraryDetailResponse itinerary
) {
}
