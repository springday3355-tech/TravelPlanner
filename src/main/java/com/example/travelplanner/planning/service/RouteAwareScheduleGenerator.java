package com.example.travelplanner.planning.service;

import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.place.dto.OpeningHourResponse;
import com.example.travelplanner.place.entity.PlaceCategory;
import com.example.travelplanner.planning.entity.TransportMode;
import com.example.travelplanner.planning.route.RouteMatrixService;
import com.example.travelplanner.planning.route.RouteMatrixSnapshot;
import com.example.travelplanner.planning.route.RouteMetric;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class RouteAwareScheduleGenerator implements ScheduleGenerator {

    private static final int DEFAULT_TRAVEL_MINUTES = 30;
    private static final double ESTIMATED_URBAN_SPEED_KMH = 20.0;
    private final RouteMatrixService routeMatrixService;

    @Override
    public PlanGenerationResult generate(PlanGenerationContext context) {
        RouteMatrixSnapshot routeMatrix = routeMatrixService.resolve(context);
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
        boolean routeFallbackUsed = false;

        for (int dayIndex = 0; dayIndex < dayCount; dayIndex++) {
            LocalDate date = context.startDate().plusDays(dayIndex);
            LocalTime cursor = context.dailyStartTime();
            Position position = Position.start(context.startLatitude(), context.startLongitude());

            for (int order = 1; order <= visitsPerDay; order++) {
                CandidateChoice choice = chooseCandidate(
                        remaining, context, routeMatrix, position, date, cursor, totalCost
                );
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
                position = Position.afterVisit(place.id().toString(), place.latitude(), place.longitude());
                totalCost += valueOrZero(place.estimatedCost());
                totalTravelMinutes += choice.travelMinutes();
                totalDistanceKm += choice.distanceKm();
                routeFallbackUsed |= choice.routeFallbackUsed();
            }
        }

        Set<Long> scheduledIds = visits.stream().map(PlannedVisit::placeId).collect(Collectors.toSet());
        Set<Long> unscheduledRequired = new HashSet<>(context.requiredPlaceIds());
        unscheduledRequired.removeAll(scheduledIds);

        List<String> warnings = buildWarnings(
                context, routeMatrix, visits, remaining, totalCost, routeFallbackUsed
        );
        return new PlanGenerationResult(
                routeMatrix.usesExternalRoutes() ? "ROUTE_MATRIX_V4" : "ROUTE_AWARE_V2",
                totalSlots, List.copyOf(visits), warnings,
                roundDistance(totalDistanceKm), totalTravelMinutes, totalCost, Set.copyOf(unscheduledRequired)
        );
    }

    private CandidateChoice chooseCandidate(
            List<PlaceResponse> remaining,
            PlanGenerationContext context,
            RouteMatrixSnapshot routeMatrix,
            Position position,
            LocalDate date,
            LocalTime cursor,
            int currentCost
    ) {
        Set<String> preferred = context.preferredCategories().stream().map(Enum::name).collect(Collectors.toSet());
        return remaining.stream()
                .filter(place -> context.requiredPlaceIds().contains(place.id())
                        || !exceedsBudget(context.budget(), currentCost, place.estimatedCost()))
                .map(place -> toChoice(place, context, routeMatrix, position, date, cursor))
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
            RouteMatrixSnapshot routeMatrix,
            Position position,
            LocalDate date,
            LocalTime cursor
    ) {
        RouteMetric route = routeMatrix.find(position.key(), place.id().toString()).orElse(null);
        double distanceKm = route == null ? calculateDistance(position, place) : route.distanceKm();
        int travelMinutes = route == null
                ? estimateTravelMinutes(position, place, distanceKm)
                : route.durationMinutes();
        LocalTime arrival = cursor.plusMinutes(travelMinutes);
        if (travelMinutes > 0 && arrival.isBefore(cursor)) {
            return null;
        }
        VisitTime visitTime = findVisitTime(place, date, arrival, context.dailyEndTime());
        if (visitTime == null) {
            return null;
        }
        boolean fallbackUsed = route == null && position.hasPrevious();
        return new CandidateChoice(
                place, visitTime.start(), visitTime.end(), travelMinutes, distanceKm, fallbackUsed
        );
    }

    private VisitTime findVisitTime(
            PlaceResponse place,
            LocalDate date,
            LocalTime arrival,
            LocalTime dailyEndTime
    ) {
        int duration = visitDuration(place);
        if (place.openingHours() == null || place.openingHours().isEmpty()) {
            LocalTime start = laterOf(arrival, place.openingTime());
            LocalTime end = start.plusMinutes(duration);
            if (!end.isAfter(start) || end.isAfter(dailyEndTime)) {
                return null;
            }
            if (place.closingTime() != null && end.isAfter(place.closingTime())) {
                return null;
            }
            return new VisitTime(start, end);
        }

        return openingWindows(place.openingHours(), date).stream()
                .map(window -> fitVisit(window, arrival, dailyEndTime, duration))
                .filter(Objects::nonNull)
                .min(Comparator.comparing(VisitTime::start))
                .orElse(null);
    }

    private List<OpeningWindow> openingWindows(List<OpeningHourResponse> openingHours, LocalDate date) {
        List<OpeningWindow> windows = new ArrayList<>();
        openingHours.stream()
                .filter(hours -> hours.dayOfWeek() == date.getDayOfWeek())
                .forEach(hours -> windows.add(new OpeningWindow(
                        hours.openingTime(),
                        hours.closesNextDay() ? LocalTime.MAX : hours.closingTime()
                )));
        openingHours.stream()
                .filter(OpeningHourResponse::closesNextDay)
                .filter(hours -> hours.dayOfWeek() == date.minusDays(1).getDayOfWeek())
                .forEach(hours -> windows.add(new OpeningWindow(LocalTime.MIN, hours.closingTime())));
        return windows;
    }

    private VisitTime fitVisit(OpeningWindow window, LocalTime arrival, LocalTime dailyEndTime, int duration) {
        LocalTime start = laterOf(arrival, window.openingTime());
        LocalTime end = start.plusMinutes(duration);
        if (!end.isAfter(start) || end.isAfter(dailyEndTime) || end.isAfter(window.closingTime())) {
            return null;
        }
        return new VisitTime(start, end);
    }

    private List<String> buildWarnings(
            PlanGenerationContext context,
            RouteMatrixSnapshot routeMatrix,
            List<PlannedVisit> visits,
            List<PlaceResponse> remaining,
            int totalCost,
            boolean routeFallbackUsed
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
        if (routeMatrix.usesExternalRoutes()) {
            warnings.add("이동 시간과 거리는 Google Routes 도로망 기준입니다.");
        }
        switch (routeMatrix.status()) {
            case DISABLED -> warnings.add("경로 API가 비활성화되어 직선거리 기준 추정치를 사용했습니다.");
            case COST_LIMIT -> warnings.add("경로 조회 비용 제한을 초과해 직선거리 기준 추정치를 사용했습니다.");
            case FAILED -> warnings.add("경로 서비스 장애로 직선거리 기준 추정치를 사용했습니다.");
            case NO_COORDINATES -> warnings.add("경로를 계산할 좌표가 없어 직선거리 기준 추정치를 사용했습니다.");
            case EXTERNAL, CACHE -> {
                if (routeFallbackUsed) {
                    warnings.add("일부 경로 결과가 없어 해당 구간은 직선거리 기준 추정치를 사용했습니다.");
                }
            }
        }
        if (context.transportMode() == TransportMode.WALK) {
            warnings.add("도보 경로 기능은 Google Routes 베타 제공 범위에 포함됩니다.");
        }
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

    private record Position(String key, BigDecimal latitude, BigDecimal longitude, boolean hasPrevious) {
        static Position start(BigDecimal latitude, BigDecimal longitude) {
            boolean hasCoordinates = latitude != null && longitude != null;
            return new Position(
                    hasCoordinates ? RouteMatrixService.START_KEY : null,
                    latitude, longitude, hasCoordinates
            );
        }

        static Position afterVisit(String key, BigDecimal latitude, BigDecimal longitude) {
            return new Position(key, latitude, longitude, true);
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
            double distanceKm,
            boolean routeFallbackUsed
    ) {
    }

    private record OpeningWindow(LocalTime openingTime, LocalTime closingTime) {
    }

    private record VisitTime(LocalTime start, LocalTime end) {
    }
}
