package com.example.travelplanner.place.dto;

import com.example.travelplanner.place.entity.PlaceOpeningHour;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record OpeningHourResponse(
        DayOfWeek dayOfWeek,
        LocalTime openingTime,
        LocalTime closingTime,
        boolean closesNextDay
) {
    public static OpeningHourResponse from(PlaceOpeningHour openingHour) {
        return new OpeningHourResponse(
                openingHour.getDayOfWeek(),
                openingHour.getOpeningTime(),
                openingHour.getClosingTime(),
                openingHour.isClosesNextDay()
        );
    }
}
