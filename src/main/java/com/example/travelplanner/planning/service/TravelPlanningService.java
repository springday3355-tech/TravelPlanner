package com.example.travelplanner.planning.service;

import com.example.travelplanner.planning.dto.GenerateTravelPlanRequest;
import com.example.travelplanner.planning.dto.GeneratedTravelPlanResponse;

public interface TravelPlanningService {

    GeneratedTravelPlanResponse generate(Long memberId, GenerateTravelPlanRequest request);
}
