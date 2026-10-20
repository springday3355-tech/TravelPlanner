package com.example.travelplanner.place.provider;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ProviderOpeningHour(
        DayOfWeek dayOfWeek,
        LocalTime openingTime,
        LocalTime closingTime,
        boolean closesNextDay
) {
}
