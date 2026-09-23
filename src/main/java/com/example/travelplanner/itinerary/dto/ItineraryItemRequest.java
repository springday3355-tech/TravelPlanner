package com.example.travelplanner.itinerary.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalTime;

/**
 * 방문 항목 추가(POST)와 수정(PUT)에 공용으로 쓰는 요청 DTO.
 */
public record ItineraryItemRequest(
        @NotNull @Positive Long placeId,
        @NotNull @Positive Integer visitOrder,
        LocalTime startTime,
        LocalTime endTime,
        String memo
) {
}
