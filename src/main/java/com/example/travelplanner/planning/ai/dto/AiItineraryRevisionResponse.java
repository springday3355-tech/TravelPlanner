package com.example.travelplanner.planning.ai.dto;

import com.example.travelplanner.itinerary.dto.ItineraryDetailResponse;
import com.example.travelplanner.itinerary.dto.ItineraryItemChangeResponse;

import java.util.List;

public record AiItineraryRevisionResponse(
        String summary,
        String provider,
        String model,
        String promptVersion,
        List<ItineraryItemChangeResponse> changes,
        ItineraryDetailResponse itinerary
) {
}
