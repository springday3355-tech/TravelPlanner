package com.example.travelplanner.planning.entity;

public enum TravelPace {
    RELAXED(3),
    BALANCED(4),
    ACTIVE(5);

    private final int maximumVisitsPerDay;

    TravelPace(int maximumVisitsPerDay) {
        this.maximumVisitsPerDay = maximumVisitsPerDay;
    }

    public int getMaximumVisitsPerDay() {
        return maximumVisitsPerDay;
    }
}
