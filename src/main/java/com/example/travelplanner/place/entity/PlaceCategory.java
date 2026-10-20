package com.example.travelplanner.place.entity;

/**
 * docs/architecture/TravelPlanner-DB설계.md 2.6절 허용 값 고정.
 * 값을 추가할 때는 DB설계 문서와 API명세 문서를 함께 갱신한다.
 */
public enum PlaceCategory {
    FOOD,
    CAFE,
    SIGHT,
    ACCOMMODATION
}
