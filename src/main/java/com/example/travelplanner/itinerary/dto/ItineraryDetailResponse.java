package com.example.travelplanner.itinerary.dto;

import com.example.travelplanner.itinerary.entity.Itinerary;

import java.time.LocalDate;
import java.util.List;

/**
 * docs/architecture/TravelPlanner-API명세.md 4절.
 * 공유 링크(GET /api/itineraries/shared/{shareToken}) 응답에서는 participants를 null로 채워
 * 민감 정보(멤버 목록)를 노출하지 않는다 — 정책 문서 2절 "공유 링크로 접근한 사용자는 VIEWER".
 */
public record ItineraryDetailResponse(
        Long id,
        String title,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        Integer budget,
        String status,
        String shareToken,
        List<ItineraryDayResponse> days,
        List<ParticipantResponse> participants
) {
    public static ItineraryDetailResponse of(
            Itinerary itinerary,
            List<ItineraryDayResponse> days,
            List<ParticipantResponse> participants
    ) {
        return new ItineraryDetailResponse(
                itinerary.getId(),
                itinerary.getTitle(),
                itinerary.getCity(),
                itinerary.getStartDate(),
                itinerary.getEndDate(),
                itinerary.getBudget(),
                itinerary.getStatus().name(),
                itinerary.getShareToken(),
                days,
                participants
        );
    }
}
