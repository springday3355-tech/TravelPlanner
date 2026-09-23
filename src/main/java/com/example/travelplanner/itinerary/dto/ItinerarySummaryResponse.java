package com.example.travelplanner.itinerary.dto;

import com.example.travelplanner.itinerary.entity.Itinerary;
import com.example.travelplanner.itinerary.entity.ParticipantRole;

import java.time.LocalDate;

public record ItinerarySummaryResponse(
        Long id,
        String title,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String myRole
) {
    public static ItinerarySummaryResponse of(Itinerary itinerary, ParticipantRole myRole) {
        return new ItinerarySummaryResponse(
                itinerary.getId(),
                itinerary.getTitle(),
                itinerary.getCity(),
                itinerary.getStartDate(),
                itinerary.getEndDate(),
                itinerary.getStatus().name(),
                myRole.name()
        );
    }
}
