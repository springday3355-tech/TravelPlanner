package com.example.travelplanner.itinerary.service;

import com.example.travelplanner.itinerary.repository.ItineraryItemRepository;
import com.example.travelplanner.place.service.PlaceUsageChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ItineraryPlaceUsageChecker implements PlaceUsageChecker {

    private final ItineraryItemRepository itineraryItemRepository;

    @Override
    public boolean isUsed(Long placeId) {
        return itineraryItemRepository.existsByPlaceId(placeId);
    }
}
