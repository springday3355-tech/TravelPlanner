package com.example.travelplanner.place.provider;

import com.example.travelplanner.place.entity.PlaceCategory;

import java.util.Set;

public record PlaceSearchCriteria(
        String query,
        String region,
        Set<PlaceCategory> categories,
        int limit
) {
}
