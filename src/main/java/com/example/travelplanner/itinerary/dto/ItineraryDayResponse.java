package com.example.travelplanner.itinerary.dto;

import com.example.travelplanner.itinerary.entity.ItineraryDay;

import java.time.LocalDate;
import java.util.List;

public record ItineraryDayResponse(
        Long id,
        Integer dayNumber,
        LocalDate date,
        List<ItineraryItemResponse> items
) {
    public static ItineraryDayResponse of(ItineraryDay day, List<ItineraryItemResponse> items) {
        return new ItineraryDayResponse(day.getId(), day.getDayNumber(), day.getDate(), items);
    }
}
