package com.example.travelplanner.place.service;

import com.example.travelplanner.common.exception.BusinessException;
import com.example.travelplanner.common.exception.ErrorCode;
import com.example.travelplanner.place.dto.ExternalPlaceImportRequest;
import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.place.entity.PlaceCategory;
import com.example.travelplanner.place.provider.PlaceProvider;
import com.example.travelplanner.place.provider.PlaceSearchCriteria;
import com.example.travelplanner.place.provider.ProviderPlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlaceImportService {

    private static final int DEFAULT_LIMIT = 10;

    private final PlaceProvider placeProvider;
    private final PlaceImportPersistenceService persistenceService;

    public List<PlaceResponse> importPlaces(ExternalPlaceImportRequest request) {
        ensureProviderAvailable();
        int limit = request.limit() == null ? DEFAULT_LIMIT : request.limit();
        return importPlaces(request.query(), request.region(), immutableCategories(request.categories()), limit);
    }

    public List<PlaceResponse> importForPlanning(String region, Set<PlaceCategory> preferredCategories) {
        if (!placeProvider.isAvailable()) {
            return List.of();
        }
        Set<PlaceCategory> categories = immutableCategories(preferredCategories);
        return importPlaces(planningQuery(categories), region, categories, DEFAULT_LIMIT);
    }

    private List<PlaceResponse> importPlaces(
            String query,
            String region,
            Set<PlaceCategory> categories,
            int limit
    ) {
        List<ProviderPlace> places = placeProvider.search(
                new PlaceSearchCriteria(query, region, categories, limit)
        );
        return persistenceService.upsertAll(placeProvider.source(), places);
    }

    private void ensureProviderAvailable() {
        if (!placeProvider.isAvailable()) {
            throw new BusinessException(ErrorCode.PLACE_PROVIDER_UNAVAILABLE);
        }
    }

    private Set<PlaceCategory> immutableCategories(Set<PlaceCategory> categories) {
        return categories == null ? Set.of() : Set.copyOf(categories);
    }

    private String planningQuery(Set<PlaceCategory> categories) {
        if (categories.size() != 1) {
            return "여행 추천 장소";
        }
        return switch (categories.iterator().next()) {
            case FOOD -> "맛집";
            case CAFE -> "카페";
            case SIGHT -> "관광 명소";
            case ACCOMMODATION -> "숙소";
        };
    }
}
