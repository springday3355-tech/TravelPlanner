package com.example.travelplanner.itinerary.dto;

import com.example.travelplanner.itinerary.entity.ItineraryParticipant;

public record ParticipantResponse(
        Long id,
        Long memberId,
        String nickname,
        String participantRole
) {
    public static ParticipantResponse of(ItineraryParticipant participant, String nickname) {
        return new ParticipantResponse(
                participant.getId(),
                participant.getMemberId(),
                nickname,
                participant.getParticipantRole().name()
        );
    }
}
