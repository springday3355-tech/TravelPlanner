package com.example.travelplanner.planning.ai.provider;

import com.example.travelplanner.itinerary.dto.ItineraryDayResponse;
import com.example.travelplanner.place.dto.PlaceResponse;

import java.time.LocalDate;
import java.util.List;

public record AiRevisionContext(
        Long itineraryId,
        String title,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        Integer budget,
        List<ItineraryDayResponse> days,
        List<PlaceResponse> candidatePlaces
) {
}
