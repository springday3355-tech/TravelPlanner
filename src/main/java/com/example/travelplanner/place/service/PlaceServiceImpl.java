package com.example.travelplanner.place.service;

import com.example.travelplanner.common.exception.BusinessException;
import com.example.travelplanner.common.exception.ErrorCode;
import com.example.travelplanner.place.dto.PlaceRequest;
import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.place.entity.Place;
import com.example.travelplanner.place.entity.PlaceCategory;
import com.example.travelplanner.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceServiceImpl implements PlaceService {

    private final PlaceRepository placeRepository;
    private final PlaceUsageChecker placeUsageChecker;

    @Override
    public List<PlaceResponse> search(String region, PlaceCategory category) {
        List<Place> places;
        if (region != null && category != null) {
            places = placeRepository.findByRegionAndCategory(region, category);
        } else if (region != null) {
            places = placeRepository.findByRegion(region);
        } else if (category != null) {
            places = placeRepository.findByCategory(category);
        } else {
            places = placeRepository.findAll();
        }
        return places.stream().map(PlaceResponse::from).toList();
    }

    @Override
    public PlaceResponse getPlace(Long placeId) {
        return PlaceResponse.from(getPlaceOrThrow(placeId));
    }

    @Override
    @Transactional
    public PlaceResponse create(PlaceRequest request) {
        validatePlanningFields(request);
        Place place = Place.of(
                request.name(), request.category(), request.region(), request.address(),
                request.latitude(), request.longitude(), request.priceRange(), request.openHours(),
                request.openingTime(), request.closingTime(), request.estimatedCost(), request.recommendedVisitMinutes()
        );
        return PlaceResponse.from(placeRepository.save(place));
    }

    @Override
    @Transactional
    public PlaceResponse update(Long placeId, PlaceRequest request) {
        validatePlanningFields(request);
        Place place = getPlaceOrThrow(placeId);
        place.update(
                request.name(), request.category(), request.region(), request.address(),
                request.latitude(), request.longitude(), request.priceRange(), request.openHours(),
                request.openingTime(), request.closingTime(), request.estimatedCost(), request.recommendedVisitMinutes()
        );
        return PlaceResponse.from(place);
    }

    @Override
    @Transactional
    public void delete(Long placeId) {
        if (!placeRepository.existsById(placeId)) {
            throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
        }
        if (placeUsageChecker.isUsed(placeId)) {
            throw new BusinessException(ErrorCode.PLACE_IN_USE);
        }
        placeRepository.deleteById(placeId);
    }

    private Place getPlaceOrThrow(Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
    }

    private void validatePlanningFields(PlaceRequest request) {
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "장소 좌표는 위도와 경도를 함께 입력해야 합니다.");
        }
        if ((request.openingTime() == null) != (request.closingTime() == null)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "영업 시작 시간과 종료 시간을 함께 입력해야 합니다.");
        }
        if (request.openingTime() != null && !request.closingTime().isAfter(request.openingTime())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "영업 종료 시간은 시작 시간보다 늦어야 합니다.");
        }
    }
}
