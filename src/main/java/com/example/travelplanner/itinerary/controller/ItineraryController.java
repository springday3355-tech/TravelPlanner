package com.example.travelplanner.itinerary.controller;

import com.example.travelplanner.common.response.ApiResponse;
import com.example.travelplanner.common.security.SecurityUtils;
import com.example.travelplanner.itinerary.dto.AddDayRequest;
import com.example.travelplanner.itinerary.dto.InviteParticipantRequest;
import com.example.travelplanner.itinerary.dto.ItineraryDayResponse;
import com.example.travelplanner.itinerary.dto.ItineraryDetailResponse;
import com.example.travelplanner.itinerary.dto.ItineraryItemRequest;
import com.example.travelplanner.itinerary.dto.ItineraryItemResponse;
import com.example.travelplanner.itinerary.dto.CreateItineraryRequest;
import com.example.travelplanner.itinerary.dto.ItinerarySummaryResponse;
import com.example.travelplanner.itinerary.dto.ParticipantResponse;
import com.example.travelplanner.itinerary.dto.ShareLinkResponse;
import com.example.travelplanner.itinerary.dto.UpdateItineraryRequest;
import com.example.travelplanner.itinerary.service.ItineraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * docs/architecture/TravelPlanner-API명세.md 2절.
 */
@RestController
@RequestMapping("/api/itineraries")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;

    @PostMapping
    public ResponseEntity<ApiResponse<ItinerarySummaryResponse>> create(@Valid @RequestBody CreateItineraryRequest request) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        ItinerarySummaryResponse response = itineraryService.create(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ApiResponse<List<ItinerarySummaryResponse>> getMyItineraries() {
        Long memberId = SecurityUtils.getCurrentMemberId();
        return ApiResponse.success(itineraryService.getMyItineraries(memberId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ItineraryDetailResponse> getDetail(@PathVariable Long id) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        return ApiResponse.success(itineraryService.getDetail(id, memberId));
    }

    @GetMapping("/shared/{shareToken}")
    public ApiResponse<ItineraryDetailResponse> getSharedDetail(@PathVariable String shareToken) {
        return ApiResponse.success(itineraryService.getSharedDetail(shareToken));
    }

    @PutMapping("/{id}")
    public ApiResponse<ItineraryDetailResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateItineraryRequest request) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        return ApiResponse.success(itineraryService.update(id, memberId, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        itineraryService.delete(id, memberId);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/share")
    public ApiResponse<ShareLinkResponse> createShareLink(@PathVariable Long id) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        return ApiResponse.success(itineraryService.createShareLink(id, memberId));
    }

    @PostMapping("/{id}/participants")
    public ResponseEntity<ApiResponse<ParticipantResponse>> inviteParticipant(
            @PathVariable Long id, @Valid @RequestBody InviteParticipantRequest request
    ) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        ParticipantResponse response = itineraryService.inviteParticipant(id, memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}/participants/{participantId}")
    public ApiResponse<Void> removeParticipant(@PathVariable Long id, @PathVariable Long participantId) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        itineraryService.removeParticipant(id, memberId, participantId);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/days")
    public ResponseEntity<ApiResponse<ItineraryDayResponse>> addDay(@PathVariable Long id, @Valid @RequestBody AddDayRequest request) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        ItineraryDayResponse response = itineraryService.addDay(id, memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/{id}/days/{dayId}/items")
    public ResponseEntity<ApiResponse<ItineraryItemResponse>> addItem(
            @PathVariable Long id, @PathVariable Long dayId, @Valid @RequestBody ItineraryItemRequest request
    ) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        ItineraryItemResponse response = itineraryService.addItem(id, dayId, memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}/days/{dayId}/items/{itemId}")
    public ApiResponse<ItineraryItemResponse> updateItem(
            @PathVariable Long id, @PathVariable Long dayId, @PathVariable Long itemId,
            @Valid @RequestBody ItineraryItemRequest request
    ) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        return ApiResponse.success(itineraryService.updateItem(id, dayId, itemId, memberId, request));
    }

    @DeleteMapping("/{id}/days/{dayId}/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable Long id, @PathVariable Long dayId, @PathVariable Long itemId) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        itineraryService.deleteItem(id, dayId, itemId, memberId);
        return ApiResponse.success();
    }
}
