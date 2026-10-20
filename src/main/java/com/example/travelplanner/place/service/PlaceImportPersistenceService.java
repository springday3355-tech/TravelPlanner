package com.example.travelplanner.place.service;

import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.place.entity.Place;
import com.example.travelplanner.place.entity.PlaceOpeningHour;
import com.example.travelplanner.place.entity.PlaceSource;
import com.example.travelplanner.place.provider.ProviderPlace;
import com.example.travelplanner.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceImportPersistenceService {

    private final PlaceRepository placeRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<PlaceResponse> upsertAll(PlaceSource source, List<ProviderPlace> providerPlaces) {
        LocalDateTime syncedAt = LocalDateTime.now();
        return providerPlaces.stream()
                .filter(place -> place.externalId() != null && place.name() != null)
                .map(place -> upsert(source, place, syncedAt))
                .map(PlaceResponse::from)
                .toList();
    }

    private Place upsert(PlaceSource source, ProviderPlace providerPlace, LocalDateTime syncedAt) {
        List<PlaceOpeningHour> openingHours = providerPlace.openingHours() == null
                ? List.of()
                : providerPlace.openingHours().stream()
                .map(openingHour -> PlaceOpeningHour.of(
                        openingHour.dayOfWeek(), openingHour.openingTime(),
                        openingHour.closingTime(), openingHour.closesNextDay()
                ))
                .toList();
        Place place = placeRepository.findBySourceAndExternalId(source, providerPlace.externalId())
                .orElseGet(() -> Place.imported(
                        providerPlace.name(), providerPlace.category(), providerPlace.region(), providerPlace.address(),
                        providerPlace.latitude(), providerPlace.longitude(), providerPlace.priceRange(),
                        providerPlace.openHours(), providerPlace.estimatedCost(), providerPlace.recommendedVisitMinutes(),
                        source, providerPlace.externalId(), syncedAt, openingHours
                ));
        if (place.getId() != null) {
            place.synchronize(
                    providerPlace.name(), providerPlace.category(), providerPlace.region(), providerPlace.address(),
                    providerPlace.latitude(), providerPlace.longitude(), providerPlace.priceRange(),
                    providerPlace.openHours(), providerPlace.estimatedCost(), providerPlace.recommendedVisitMinutes(),
                    syncedAt, openingHours
            );
        }
        return placeRepository.save(place);
    }
}
