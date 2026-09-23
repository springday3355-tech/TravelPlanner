package com.example.travelplanner.itinerary.dto;

import com.example.travelplanner.itinerary.entity.ParticipantRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteParticipantRequest(
        @NotBlank @Email String memberEmail,
        @NotNull ParticipantRole participantRole
) {
}
