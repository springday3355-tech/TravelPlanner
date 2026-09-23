package com.example.travelplanner.planning.service;

import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.place.entity.PlaceCategory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RuleBasedScheduleGenerator implements ScheduleGenerator {

    private static final int VISIT_MINUTES = 90;
    private static final int MOVE_MINUTES = 30;

    @Override
    public PlanGenerationResult generate(PlanGenerationContext context) {
        int dayCount = (int) ChronoUnit.DAYS.between(context.startDate(), context.endDate()) + 1;
        int slotsByTime = calculateSlotsByTime(context.dailyStartTime(), context.dailyEndTime());
        int visitsPerDay = Math.min(
                slotsByTime,
                Math.min(context.travelPace().getMaximumVisitsPerDay(), companionVisitLimit(context))
        );
        int totalSlots = dayCount * visitsPerDay;

        List<PlaceResponse> ordered = orderCandidates(
                context.candidates(), context.requiredPlaceIds(), context.preferredCategories()
        );
        List<PlaceResponse> selected = ordered.stream().limit(totalSlots).toList();
        List<PlannedVisit> visits = new ArrayList<>();

        int candidateIndex = 0;
        for (int dayIndex = 0; dayIndex < dayCount; dayIndex++) {
            LocalDate date = context.startDate().plusDays(dayIndex);
            LocalTime startTime = context.dailyStartTime();
            for (int order = 1; order <= visitsPerDay && candidateIndex < selected.size(); order++) {
                PlaceResponse place = selected.get(candidateIndex++);
                LocalTime endTime = startTime.plusMinutes(VISIT_MINUTES);
                visits.add(new PlannedVisit(dayIndex + 1, date, place.id(), order, startTime, endTime));
                startTime = endTime.plusMinutes(MOVE_MINUTES);
            }
        }

        List<String> warnings = new ArrayList<>();
        if (ordered.size() > totalSlots) {
            warnings.add("방문 가능 시간보다 후보 장소가 많아 우선순위가 높은 장소만 포함했습니다.");
        }
        if (ordered.size() < totalSlots) {
            warnings.add("등록된 장소가 부족해 일부 시간대는 비워 두었습니다.");
        }
        warnings.add("현재 초안은 장소 간 실제 이동 시간과 영업시간을 반영하지 않습니다.");
        warnings.add("예산은 일정 정보로 저장되며 장소 선택 비용에는 아직 반영되지 않습니다.");

        return new PlanGenerationResult("RULE_BASED_V1", totalSlots, visits, List.copyOf(warnings));
    }

    private int calculateSlotsByTime(LocalTime start, LocalTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < VISIT_MINUTES) {
            return 0;
        }
        return (int) ((minutes + MOVE_MINUTES) / (VISIT_MINUTES + MOVE_MINUTES));
    }

    private int companionVisitLimit(PlanGenerationContext context) {
        return switch (context.companionType()) {
            case FAMILY -> 3;
            case COUPLE -> 4;
            case SOLO, FRIENDS -> 5;
        };
    }

    private List<PlaceResponse> orderCandidates(
            List<PlaceResponse> candidates,
            Set<Long> requiredPlaceIds,
            Set<PlaceCategory> preferredCategories
    ) {
        Set<String> preferred = preferredCategories.stream().map(Enum::name).collect(Collectors.toSet());
        return candidates.stream()
                .sorted(Comparator
                        .comparing((PlaceResponse place) -> !requiredPlaceIds.contains(place.id()))
                        .thenComparing(place -> !preferred.contains(place.category()))
                        .thenComparing(PlaceResponse::id))
                .toList();
    }
}
