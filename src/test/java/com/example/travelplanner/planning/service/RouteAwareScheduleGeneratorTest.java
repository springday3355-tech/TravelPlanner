package com.example.travelplanner.planning.service;

import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.place.entity.PlaceCategory;
import com.example.travelplanner.planning.entity.CompanionType;
import com.example.travelplanner.planning.entity.TransportMode;
import com.example.travelplanner.planning.entity.TravelPace;
import com.example.travelplanner.planning.route.RouteMatrixProvider;
import com.example.travelplanner.planning.route.RouteMatrixService;
import com.example.travelplanner.planning.route.RouteMetric;
import com.example.travelplanner.planning.route.RoutePair;
import com.example.travelplanner.planning.route.RoutePoint;
import com.example.travelplanner.planning.route.google.GoogleRoutesProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteAwareScheduleGeneratorTest {
    @Test
    @DisplayName("실제 이동시간 때문에 방문할 수 없는 필수 장소를 일정에 억지로 넣지 않는다")
    void generate_rejectsImpossibleRouteTiming() {
        RouteAwareScheduleGenerator generator = generator(new RouteMatrixProvider() {
            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public Map<RoutePair, RouteMetric> compute(
                    List<RoutePoint> origins, List<RoutePoint> destinations, TransportMode mode
            ) {
                return Map.of(new RoutePair("START", "1"), new RouteMetric(120, 80));
            }
        });

        PlanGenerationResult result = generator.generate(context(Set.of(1L)));

        assertEquals("ROUTE_MATRIX_V4", result.generator());
        assertTrue(result.visits().isEmpty());
        assertEquals(Set.of(1L), result.unscheduledRequiredPlaceIds());
    }

    @Test
    @DisplayName("경로 API가 실패해도 직선거리 추정으로 일정을 생성한다")
    void generate_fallsBackWhenProviderFails() {
        RouteAwareScheduleGenerator generator = generator(new RouteMatrixProvider() {
            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public Map<RoutePair, RouteMetric> compute(
                    List<RoutePoint> origins, List<RoutePoint> destinations, TransportMode mode
            ) {
                throw new IllegalStateException("temporary failure");
            }
        });

        PlanGenerationResult result = generator.generate(context(Set.of()));

        assertEquals("ROUTE_AWARE_V2", result.generator());
        assertEquals(1, result.visits().size());
        assertTrue(result.warnings().contains("경로 서비스 장애로 직선거리 기준 추정치를 사용했습니다."));
    }

    private RouteAwareScheduleGenerator generator(RouteMatrixProvider provider) {
        GoogleRoutesProperties properties = new GoogleRoutesProperties();
        properties.setMaxElementsPerPlan(10);
        return new RouteAwareScheduleGenerator(new RouteMatrixService(provider, properties));
    }

    private PlanGenerationContext context(Set<Long> requiredIds) {
        LocalDate date = LocalDate.of(2026, 11, 1);
        return new PlanGenerationContext(
                date, date, LocalTime.of(9, 0), LocalTime.of(10, 30), Set.of(PlaceCategory.SIGHT),
                CompanionType.SOLO, TravelPace.BALANCED, requiredIds, List.of(place()), 100000,
                new BigDecimal("37.5"), new BigDecimal("127.0"), TransportMode.DRIVE
        );
    }

    private PlaceResponse place() {
        return new PlaceResponse(
                1L, "필수 장소", "SIGHT", "서울", null,
                new BigDecimal("37.501"), new BigDecimal("127.001"), null, null,
                LocalTime.of(9, 0), LocalTime.of(18, 0), 0, 60,
                "MANUAL", null, null, List.of()
        );
    }
}
