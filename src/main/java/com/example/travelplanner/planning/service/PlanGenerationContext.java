package com.example.travelplanner.planning.service;

import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.place.entity.PlaceCategory;
import com.example.travelplanner.planning.entity.CompanionType;
import com.example.travelplanner.planning.entity.TravelPace;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record PlanGenerationContext(
        LocalDate startDate,
        LocalDate endDate,
        LocalTime dailyStartTime,
        LocalTime dailyEndTime,
        Set<PlaceCategory> preferredCategories,
        CompanionType companionType,
        TravelPace travelPace,
        Set<Long> requiredPlaceIds,
        List<PlaceResponse> candidates,
        Integer budget,
        BigDecimal startLatitude,
        BigDecimal startLongitude
) {
}
