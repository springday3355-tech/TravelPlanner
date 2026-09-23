package com.example.travelplanner.planning.service;

import java.util.List;

public record PlanGenerationResult(
        String generator,
        int availableSlotCount,
        List<PlannedVisit> visits,
        List<String> warnings
) {
}
