package com.example.travelplanner.planning.service;

import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.place.entity.PlaceCategory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RouteAwareScheduleGenerator implements ScheduleGenerator {

    private static final int DEFAULT_TRAVEL_MINUTES = 30;
    private static final double ESTIMATED_URBAN_SPEED_KMH = 20.0;

    @Override
    public PlanGenerationResult generate(PlanGenerationContext context) {
        int dayCount = (int) ChronoUnit.DAYS.between(context.startDate(), context.endDate()) + 1;
        int visitsPerDay = Math.min(
                context.travelPace().getMaximumVisitsPerDay(), companionVisitLimit(context)
        );
        int totalSlots = dayCount * visitsPerDay;
        List<PlaceResponse> remaining = new ArrayList<>(context.candidates());
        List<PlannedVisit> visits = new ArrayList<>();
        int totalCost = 0;
        int totalTravelMinutes = 0;
        double totalDistanceKm = 0;

        for (int dayIndex = 0; dayIndex < dayCount; dayIndex++) {
            LocalDate date = context.startDate().plusDays(dayIndex);
            LocalTime cursor = context.dailyStartTime();
            Position position = Position.start(context.startLatitude(), context.startLongitude());

            for (int order = 1; order <= visitsPerDay; order++) {
                CandidateChoice choice = chooseCandidate(remaining, context, position, cursor, totalCost);
                if (choice == null) {
                    break;
                }
                PlaceResponse place = choice.place();
                visits.add(new PlannedVisit(
                        dayIndex + 1, date, place.id(), order, choice.startTime(), choice.endTime(),
                        choice.travelMinutes(), choice.distanceKm(), place.estimatedCost()
                ));
                remaining.remove(place);
                cursor = choice.endTime();
                position = Position.afterVisit(place.latitude(), place.longitude());
                totalCost += valueOrZero(place.estimatedCost());
                totalTravelMinutes += choice.travelMinutes();
                totalDistanceKm += choice.distanceKm();
            }
        }

        Set<Long> scheduledIds = visits.stream().map(PlannedVisit::placeId).collect(Collectors.toSet());
        Set<Long> unscheduledRequired = new HashSet<>(context.requiredPlaceIds());
        unscheduledRequired.removeAll(scheduledIds);

        List<String> warnings = buildWarnings(context, visits, remaining, totalCost);
        return new PlanGenerationResult(
                "ROUTE_AWARE_V2", totalSlots, List.copyOf(visits), warnings,
                roundDistance(totalDistanceKm), totalTravelMinutes, totalCost, Set.copyOf(unscheduledRequired)
        );
    }

    private CandidateChoice chooseCandidate(
            List<PlaceResponse> remaining,
            PlanGenerationContext context,
            Position position,
            LocalTime cursor,
            int currentCost
    ) {
        Set<String> preferred = context.preferredCategories().stream().map(Enum::name).collect(Collectors.toSet());
        return remaining.stream()
                .filter(place -> context.requiredPlaceIds().contains(place.id())
                        || !exceedsBudget(context.budget(), currentCost, place.estimatedCost()))
                .map(place -> toChoice(place, context, position, cursor))
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((CandidateChoice choice) -> !context.requiredPlaceIds().contains(choice.place().id()))
                        .thenComparing(choice -> !preferred.contains(choice.place().category()))
                        .thenComparing(CandidateChoice::startTime)
                        .thenComparingDouble(CandidateChoice::distanceKm)
                        .thenComparing(choice -> choice.place().id()))
                .findFirst()
                .orElse(null);
    }

    private CandidateChoice toChoice(
            PlaceResponse place,
            PlanGenerationContext context,
            Position position,
            LocalTime cursor
    ) {
        double distanceKm = calculateDistance(position, place);
        int travelMinutes = estimateTravelMinutes(position, place, distanceKm);
        LocalTime arrival = cursor.plusMinutes(travelMinutes);
        if (travelMinutes > 0 && arrival.isBefore(cursor)) {
            return null;
        }
        LocalTime start = laterOf(arrival, place.openingTime());
        LocalTime end = start.plusMinutes(visitDuration(place));
        if (!end.isAfter(start) || end.isAfter(context.dailyEndTime())) {
            return null;
        }
        if (place.closingTime() != null && end.isAfter(place.closingTime())) {
            return null;
        }
        return new CandidateChoice(place, start, end, travelMinutes, distanceKm);
    }

    private List<String> buildWarnings(
            PlanGenerationContext context,
            List<PlannedVisit> visits,
            List<PlaceResponse> remaining,
            int totalCost
    ) {
        List<String> warnings = new ArrayList<>();
        if (!remaining.isEmpty()) {
            warnings.add("일정 시간·영업시간·예산 조건으로 일부 후보 장소를 포함하지 못했습니다.");
        }
        boolean missingCoordinates = visits.stream()
                .map(visit -> context.candidates().stream()
                        .filter(place -> place.id().equals(visit.placeId())).findFirst().orElseThrow())
                .anyMatch(place -> place.latitude() == null || place.longitude() == null);
        if (missingCoordinates) {
            warnings.add("좌표가 없는 장소의 이동 시간은 30분으로 계산했습니다.");
        }
        if (context.budget() != null && visits.stream().anyMatch(visit -> visit.estimatedCost() == null)) {
            warnings.add("예상 비용이 없는 장소는 예산 합계에서 제외했습니다.");
        }
        if (context.budget() != null && totalCost > context.budget()) {
            warnings.add("필수 장소를 포함하기 위해 예상 비용이 입력 예산을 초과했습니다.");
        }
        warnings.add("이동 시간은 직선거리와 평균 시속 20km를 기준으로 한 추정치입니다.");
        return List.copyOf(warnings);
    }

    private int companionVisitLimit(PlanGenerationContext context) {
        return switch (context.companionType()) {
            case FAMILY -> 3;
            case COUPLE -> 4;
            case SOLO, FRIENDS -> 5;
        };
    }

    private int visitDuration(PlaceResponse place) {
        if (place.recommendedVisitMinutes() != null) {
            return place.recommendedVisitMinutes();
        }
        return switch (PlaceCategory.valueOf(place.category())) {
            case FOOD -> 90;
            case CAFE -> 60;
            case SIGHT -> 90;
            case ACCOMMODATION -> 60;
        };
    }

    private boolean exceedsBudget(Integer budget, int currentCost, Integer placeCost) {
        return budget != null && placeCost != null && currentCost + placeCost > budget;
    }

    private LocalTime laterOf(LocalTime first, LocalTime second) {
        return second != null && second.isAfter(first) ? second : first;
    }

    private double calculateDistance(Position position, PlaceResponse place) {
        if (!position.hasPrevious() || !position.hasCoordinates()
                || place.latitude() == null || place.longitude() == null) {
            return 0;
        }
        return haversine(position.latitude(), position.longitude(), place.latitude(), place.longitude());
    }

    private int estimateTravelMinutes(Position position, PlaceResponse place, double distanceKm) {
        if (!position.hasPrevious()) {
            if (position.hasCoordinates() && place.latitude() != null && place.longitude() != null) {
                return minutesForDistance(distanceKm);
            }
            return 0;
        }
        if (!position.hasCoordinates() || place.latitude() == null || place.longitude() == null) {
            return DEFAULT_TRAVEL_MINUTES;
        }
        return minutesForDistance(distanceKm);
    }

    private int minutesForDistance(double distanceKm) {
        if (distanceKm == 0) {
            return 0;
        }
        return Math.max(10, (int) Math.ceil(distanceKm / ESTIMATED_URBAN_SPEED_KMH * 60));
    }

    private double haversine(BigDecimal fromLat, BigDecimal fromLon, BigDecimal toLat, BigDecimal toLon) {
        double lat1 = Math.toRadians(fromLat.doubleValue());
        double lat2 = Math.toRadians(toLat.doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(toLon.doubleValue() - fromLon.doubleValue());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private double roundDistance(double distance) {
        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private record Position(BigDecimal latitude, BigDecimal longitude, boolean hasPrevious) {
        static Position start(BigDecimal latitude, BigDecimal longitude) {
            return new Position(latitude, longitude, latitude != null && longitude != null);
        }

        static Position afterVisit(BigDecimal latitude, BigDecimal longitude) {
            return new Position(latitude, longitude, true);
        }

        boolean hasCoordinates() {
            return latitude != null && longitude != null;
        }
    }

    private record CandidateChoice(
            PlaceResponse place,
            LocalTime startTime,
            LocalTime endTime,
            int travelMinutes,
            double distanceKm
    ) {
    }
}
