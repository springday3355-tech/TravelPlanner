package com.example.travelplanner.itinerary.dto;

import java.util.List;

public record ItineraryBatchChangeResult(
        List<ItineraryItemChangeResponse> changes,
        ItineraryDetailResponse itinerary
) {
}
