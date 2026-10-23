package com.example.travelplanner.itinerary.dto;

import java.time.LocalTime;

public record ItineraryItemChangeResponse(
        ItemChangeOperation operation,
        Long itemId,
        Long beforePlaceId,
        Long afterPlaceId,
        LocalTime beforeStartTime,
        LocalTime afterStartTime,
        String reason
) {
}
