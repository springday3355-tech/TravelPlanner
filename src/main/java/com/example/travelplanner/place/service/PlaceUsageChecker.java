package com.example.travelplanner.place.service;

/**
 * 장소 삭제 전에 다른 도메인에서 해당 장소를 참조하는지 확인하는 포트.
 */
public interface PlaceUsageChecker {

    boolean isUsed(Long placeId);
}
