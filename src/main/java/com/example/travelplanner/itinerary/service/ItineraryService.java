package com.example.travelplanner.itinerary.service;

import com.example.travelplanner.itinerary.dto.AddDayRequest;
import com.example.travelplanner.itinerary.dto.CreateItineraryRequest;
import com.example.travelplanner.itinerary.dto.InviteParticipantRequest;
import com.example.travelplanner.itinerary.dto.ItineraryDayResponse;
import com.example.travelplanner.itinerary.dto.ItineraryDetailResponse;
import com.example.travelplanner.itinerary.dto.ItineraryItemRequest;
import com.example.travelplanner.itinerary.dto.ItineraryItemResponse;
import com.example.travelplanner.itinerary.dto.ItinerarySummaryResponse;
import com.example.travelplanner.itinerary.dto.ParticipantResponse;
import com.example.travelplanner.itinerary.dto.ShareLinkResponse;
import com.example.travelplanner.itinerary.dto.UpdateItineraryRequest;

import java.util.List;

/**
 * docs/architecture/TravelPlanner-API명세.md 2절 엔드포인트와 1:1로 대응한다.
 */
public interface ItineraryService {

    ItinerarySummaryResponse create(Long ownerId, CreateItineraryRequest request);

    List<ItinerarySummaryResponse> getMyItineraries(Long memberId);

    ItineraryDetailResponse getDetail(Long itineraryId, Long memberId);

    ItineraryDetailResponse getSharedDetail(String shareToken);

    ItineraryDetailResponse update(Long itineraryId, Long memberId, UpdateItineraryRequest request);

    void delete(Long itineraryId, Long memberId);

    ShareLinkResponse createShareLink(Long itineraryId, Long memberId);

    ParticipantResponse inviteParticipant(Long itineraryId, Long memberId, InviteParticipantRequest request);

    void removeParticipant(Long itineraryId, Long memberId, Long participantId);

    ItineraryDayResponse addDay(Long itineraryId, Long memberId, AddDayRequest request);

    ItineraryItemResponse addItem(Long itineraryId, Long dayId, Long memberId, ItineraryItemRequest request);

    ItineraryItemResponse updateItem(Long itineraryId, Long dayId, Long itemId, Long memberId, ItineraryItemRequest request);

    void deleteItem(Long itineraryId, Long dayId, Long itemId, Long memberId);
}
