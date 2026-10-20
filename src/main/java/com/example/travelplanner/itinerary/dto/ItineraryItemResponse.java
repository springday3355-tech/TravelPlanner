package com.example.travelplanner.itinerary.dto;

import com.example.travelplanner.itinerary.entity.ItineraryItem;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ItineraryItemResponse(
        Long id,
        Long placeId,
        String placeName,
        Integer visitOrder,
        LocalTime startTime,
        LocalTime endTime,
        String memo,
        Integer travelMinutesBefore,
        BigDecimal distanceKmBefore,
        Integer estimatedCost
) {
    public static ItineraryItemResponse of(ItineraryItem item, String placeName) {
        return new ItineraryItemResponse(
                item.getId(),
                item.getPlaceId(),
                placeName,
                item.getVisitOrder(),
                item.getStartTime(),
                item.getEndTime(),
                item.getMemo(),
                item.getTravelMinutesBefore(),
                item.getDistanceKmBefore(),
                item.getEstimatedCost()
        );
    }
}
