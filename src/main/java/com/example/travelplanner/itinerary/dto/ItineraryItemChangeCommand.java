package com.example.travelplanner.itinerary.dto;

import java.time.LocalTime;

public record ItineraryItemChangeCommand(
        ItemChangeOperation operation,
        Long dayId,
        Long itemId,
        Long placeId,
        LocalTime startTime,
        LocalTime endTime,
        String memo,
        String reason
) {
}
