package com.example.travelplanner.planning.ai.provider;

public interface ItineraryRevisionProvider {
    boolean isAvailable();

    String name();

    String model();

    String promptVersion();

    AiRevisionProposal propose(AiRevisionContext context, String instruction);
}
