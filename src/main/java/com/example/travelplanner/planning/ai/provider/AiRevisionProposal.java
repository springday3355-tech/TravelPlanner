package com.example.travelplanner.planning.ai.provider;

import com.example.travelplanner.itinerary.dto.ItineraryItemChangeCommand;

import java.util.List;

public record AiRevisionProposal(
        String summary,
        List<ItineraryItemChangeCommand> commands
) {
}
