package com.example.travelplanner.planning.service;

import java.time.LocalDate;
import java.time.LocalTime;

public record PlannedVisit(
        int dayNumber,
        LocalDate date,
        long placeId,
        int visitOrder,
        LocalTime startTime,
        LocalTime endTime,
        int travelMinutesBefore,
        double distanceKmBefore,
        Integer estimatedCost
) {
}
