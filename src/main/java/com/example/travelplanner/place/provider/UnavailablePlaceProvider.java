package com.example.travelplanner.place.provider;

import com.example.travelplanner.place.entity.PlaceSource;

import java.util.List;

public class UnavailablePlaceProvider implements PlaceProvider {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public PlaceSource source() {
        return PlaceSource.GOOGLE_PLACES;
    }

    @Override
    public List<ProviderPlace> search(PlaceSearchCriteria criteria) {
        return List.of();
    }
}
