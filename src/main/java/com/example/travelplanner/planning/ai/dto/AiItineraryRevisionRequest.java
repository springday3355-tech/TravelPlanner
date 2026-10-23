package com.example.travelplanner.planning.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiItineraryRevisionRequest(
        @NotBlank @Size(max = 1000) String instruction
) {
}
