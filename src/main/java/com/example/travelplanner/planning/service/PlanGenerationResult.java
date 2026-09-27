package com.example.travelplanner.planning.service;

import java.util.List;
import java.util.Set;

public record PlanGenerationResult(
        String generator,
        int availableSlotCount,
        List<PlannedVisit> visits,
        List<String> warnings,
        double estimatedDistanceKm,
        int estimatedTravelMinutes,
        int estimatedCost,
        Set<Long> unscheduledRequiredPlaceIds
) {
}
