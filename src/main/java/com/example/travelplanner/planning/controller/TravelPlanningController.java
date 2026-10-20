package com.example.travelplanner.planning.controller;

import com.example.travelplanner.common.response.ApiResponse;
import com.example.travelplanner.common.security.SecurityUtils;
import com.example.travelplanner.planning.dto.GenerateTravelPlanRequest;
import com.example.travelplanner.planning.dto.GeneratedTravelPlanResponse;
import com.example.travelplanner.planning.service.TravelPlanningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/travel-plans")
@RequiredArgsConstructor
public class TravelPlanningController {

    private final TravelPlanningService travelPlanningService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<GeneratedTravelPlanResponse>> generate(
            @Valid @RequestBody GenerateTravelPlanRequest request
    ) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        GeneratedTravelPlanResponse response = travelPlanningService.generate(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
