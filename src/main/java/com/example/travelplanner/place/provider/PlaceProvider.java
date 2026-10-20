package com.example.travelplanner.place.provider;

import com.example.travelplanner.place.entity.PlaceSource;

import java.util.List;

public interface PlaceProvider {

    boolean isAvailable();

    PlaceSource source();

    List<ProviderPlace> search(PlaceSearchCriteria criteria);
}
