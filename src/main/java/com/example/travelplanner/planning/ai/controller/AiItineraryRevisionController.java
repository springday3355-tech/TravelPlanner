package com.example.travelplanner.planning.ai.controller;

import com.example.travelplanner.common.response.ApiResponse;
import com.example.travelplanner.common.security.SecurityUtils;
import com.example.travelplanner.planning.ai.dto.AiItineraryRevisionRequest;
import com.example.travelplanner.planning.ai.dto.AiItineraryRevisionResponse;
import com.example.travelplanner.planning.ai.service.AiItineraryRevisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/itineraries")
@RequiredArgsConstructor
public class AiItineraryRevisionController {
    private final AiItineraryRevisionService revisionService;

    @PostMapping("/{itineraryId}/ai-revisions")
    public ApiResponse<AiItineraryRevisionResponse> revise(
            @PathVariable Long itineraryId,
            @Valid @RequestBody AiItineraryRevisionRequest request
    ) {
        return ApiResponse.success(revisionService.revise(
                itineraryId, SecurityUtils.getCurrentMemberId(), request.instruction()
        ));
    }
}
