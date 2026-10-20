package com.example.travelplanner.place.service;

import com.example.travelplanner.place.dto.PlaceRequest;
import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.place.entity.PlaceCategory;

import java.util.List;

/**
 * 다른 도메인(itinerary)은 이 인터페이스를 통해서만 장소 정보에 접근한다(SA 문서 3절).
 */
public interface PlaceService {

    List<PlaceResponse> search(String region, PlaceCategory category);

    PlaceResponse getPlace(Long placeId);

    PlaceResponse create(PlaceRequest request);

    PlaceResponse update(Long placeId, PlaceRequest request);

    void delete(Long placeId);
}
