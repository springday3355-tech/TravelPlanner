package com.example.travelplanner.planning.ai.provider;

public class UnavailableItineraryRevisionProvider implements ItineraryRevisionProvider {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String name() {
        return "UNAVAILABLE";
    }

    @Override
    public String model() {
        return "UNAVAILABLE";
    }

    @Override
    public String promptVersion() {
        return "UNAVAILABLE";
    }

    @Override
    public AiRevisionProposal propose(AiRevisionContext context, String instruction) {
        throw new IllegalStateException("AI itinerary revision provider is unavailable");
    }
}
