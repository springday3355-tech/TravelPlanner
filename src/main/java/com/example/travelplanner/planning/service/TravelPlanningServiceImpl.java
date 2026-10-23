package com.example.travelplanner.planning.service;

import com.example.travelplanner.common.exception.BusinessException;
import com.example.travelplanner.common.exception.ErrorCode;
import com.example.travelplanner.itinerary.dto.AddDayRequest;
import com.example.travelplanner.itinerary.dto.CreateItineraryRequest;
import com.example.travelplanner.itinerary.dto.ItineraryDayResponse;
import com.example.travelplanner.itinerary.dto.ItineraryItemRequest;
import com.example.travelplanner.itinerary.dto.ItinerarySummaryResponse;
import com.example.travelplanner.itinerary.service.ItineraryService;
import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.place.entity.PlaceCategory;
import com.example.travelplanner.place.service.PlaceService;
import com.example.travelplanner.planning.dto.GenerateTravelPlanRequest;
import com.example.travelplanner.planning.dto.GeneratedTravelPlanResponse;
import com.example.travelplanner.planning.entity.TransportMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TravelPlanningServiceImpl implements TravelPlanningService {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(18, 0);
    private static final int MAX_TRIP_DAYS = 14;

    private final ItineraryService itineraryService;
    private final PlaceService placeService;
    private final ScheduleGenerator scheduleGenerator;

    @Override
    @Transactional
    public GeneratedTravelPlanResponse generate(Long memberId, GenerateTravelPlanRequest request) {
        validateRequest(request);

        LocalTime dailyStartTime = request.dailyStartTime() == null ? DEFAULT_START_TIME : request.dailyStartTime();
        LocalTime dailyEndTime = request.dailyEndTime() == null ? DEFAULT_END_TIME : request.dailyEndTime();
        if (!dailyEndTime.isAfter(dailyStartTime)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "하루 종료 시간은 시작 시간보다 늦어야 합니다.");
        }
        if (Duration.between(dailyStartTime, dailyEndTime).toMinutes() < 90) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "하루 일정 시간은 방문 장소 하나를 배치할 수 있도록 90분 이상이어야 합니다.");
        }
        if ((request.startLatitude() == null) != (request.startLongitude() == null)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "출발 좌표는 위도와 경도를 함께 입력해야 합니다.");
        }

        Set<Long> requiredPlaceIds = immutableSet(request.requiredPlaceIds());
        Set<Long> excludedPlaceIds = immutableSet(request.excludedPlaceIds());
        if (!Collections.disjoint(requiredPlaceIds, excludedPlaceIds)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "필수 장소와 제외 장소는 중복될 수 없습니다.");
        }

        Set<PlaceCategory> preferredCategories = immutableSet(request.preferredCategories());
        List<PlaceResponse> regionPlaces = placeService.searchForPlanning(request.city(), preferredCategories);
        validateRequiredPlaces(request.city(), requiredPlaceIds, regionPlaces);
        List<PlaceResponse> candidates = regionPlaces.stream()
                .filter(place -> !excludedPlaceIds.contains(place.id()))
                .toList();
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.PLANNING_PLACE_NOT_FOUND);
        }

        PlanGenerationResult generation = scheduleGenerator.generate(new PlanGenerationContext(
                request.startDate(), request.endDate(), dailyStartTime, dailyEndTime,
                preferredCategories, request.companionType(), request.travelPace(), requiredPlaceIds, candidates,
                request.budget(), request.startLatitude(), request.startLongitude(),
                request.transportMode() == null ? TransportMode.TRANSIT : request.transportMode()
        ));
        if (requiredPlaceIds.size() > generation.availableSlotCount()) {
            throw new BusinessException(ErrorCode.PLANNING_REQUIRED_PLACE_OVER_CAPACITY);
        }
        if (!generation.unscheduledRequiredPlaceIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PLANNING_REQUIRED_PLACE_UNSCHEDULABLE);
        }

        ItinerarySummaryResponse itinerary = itineraryService.create(memberId, new CreateItineraryRequest(
                request.title(), request.city(), request.startDate(), request.endDate(), request.budget()
        ));

        int dayCount = (int) ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        for (int dayNumber = 1; dayNumber <= dayCount; dayNumber++) {
            int currentDayNumber = dayNumber;
            ItineraryDayResponse day = itineraryService.addDay(
                    itinerary.id(), memberId, new AddDayRequest(dayNumber, request.startDate().plusDays(dayNumber - 1L))
            );
            generation.visits().stream()
                    .filter(visit -> visit.dayNumber() == currentDayNumber)
                    .forEach(visit -> itineraryService.addItem(
                            itinerary.id(), day.id(), memberId,
                            new ItineraryItemRequest(
                                    visit.placeId(), visit.visitOrder(), visit.startTime(), visit.endTime(),
                                    "자동 생성된 초안", visit.travelMinutesBefore(),
                                    BigDecimal.valueOf(visit.distanceKmBefore()), visit.estimatedCost()
                            )
                    ));
        }

        return new GeneratedTravelPlanResponse(
                generation.generator(), generation.visits().size(), generation.availableSlotCount(),
                generation.estimatedDistanceKm(), generation.estimatedTravelMinutes(), generation.estimatedCost(),
                generation.warnings(), itineraryService.getDetail(itinerary.id(), memberId)
        );
    }

    private void validateRequest(GenerateTravelPlanRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "종료일은 시작일보다 빠를 수 없습니다.");
        }
        long dayCount = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        if (dayCount > MAX_TRIP_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "자동 생성은 최대 14일 일정까지 지원합니다.");
        }
    }

    private void validateRequiredPlaces(String city, Set<Long> requiredPlaceIds, List<PlaceResponse> regionPlaces) {
        Set<Long> regionPlaceIds = regionPlaces.stream().map(PlaceResponse::id)
                .collect(Collectors.toSet());
        for (Long placeId : requiredPlaceIds) {
            PlaceResponse place = placeService.getPlace(placeId);
            if (!regionPlaceIds.contains(place.id())) {
                throw new BusinessException(
                        ErrorCode.PLANNING_PLACE_REGION_MISMATCH,
                        "필수 장소 '" + place.name() + "'은(는) 여행 도시 " + city + "에 속하지 않습니다."
                );
            }
        }
    }

    private <T> Set<T> immutableSet(Set<T> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

}
