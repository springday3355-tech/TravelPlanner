package com.example.travelplanner.planning.route;

import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.place.entity.PlaceCategory;
import com.example.travelplanner.planning.entity.CompanionType;
import com.example.travelplanner.planning.entity.TransportMode;
import com.example.travelplanner.planning.entity.TravelPace;
import com.example.travelplanner.planning.route.google.GoogleRoutesProperties;
import com.example.travelplanner.planning.service.PlanGenerationContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteMatrixServiceTest {
    @Test
    @DisplayName("동일한 경로 행렬은 캐시에서 재사용한다")
    void resolve_reusesCachedMatrix() {
        AtomicInteger calls = new AtomicInteger();
        RouteMatrixProvider provider = provider((origins, destinations) -> {
            calls.incrementAndGet();
            return Map.of(new RoutePair("START", "1"), new RouteMetric(15, 3.2));
        });
        RouteMatrixService service = new RouteMatrixService(provider, properties(10));

        RouteMatrixSnapshot first = service.resolve(context());
        RouteMatrixSnapshot second = service.resolve(context());

        assertEquals(RouteMatrixStatus.EXTERNAL, first.status());
        assertEquals(RouteMatrixStatus.CACHE, second.status());
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("계획당 경로 요소 제한을 넘으면 외부 API를 호출하지 않는다")
    void resolve_stopsAtCostLimit() {
        AtomicInteger calls = new AtomicInteger();
        RouteMatrixProvider provider = provider((origins, destinations) -> {
            calls.incrementAndGet();
            return Map.of();
        });

        RouteMatrixSnapshot result = new RouteMatrixService(provider, properties(1)).resolve(context());

        assertEquals(RouteMatrixStatus.COST_LIMIT, result.status());
        assertEquals(0, calls.get());
    }

    @Test
    @DisplayName("외부 경로 제공자가 실패하면 실패 상태를 반환하고 예외를 전파하지 않는다")
    void resolve_convertsProviderFailureToFallbackStatus() {
        RouteMatrixProvider provider = provider((origins, destinations) -> {
            throw new RouteProviderException("failure");
        });

        RouteMatrixSnapshot result = new RouteMatrixService(provider, properties(10)).resolve(context());

        assertEquals(RouteMatrixStatus.FAILED, result.status());
    }

    private RouteMatrixProvider provider(MatrixComputer computer) {
        return new RouteMatrixProvider() {
            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public Map<RoutePair, RouteMetric> compute(
                    List<RoutePoint> origins, List<RoutePoint> destinations, TransportMode transportMode
            ) {
                return computer.compute(origins, destinations);
            }
        };
    }

    private GoogleRoutesProperties properties(int maxElements) {
        GoogleRoutesProperties properties = new GoogleRoutesProperties();
        properties.setMaxElementsPerPlan(maxElements);
        return properties;
    }

    private PlanGenerationContext context() {
        LocalDate date = LocalDate.of(2026, 11, 1);
        return new PlanGenerationContext(
                date, date, LocalTime.of(9, 0), LocalTime.of(18, 0), Set.of(PlaceCategory.SIGHT),
                CompanionType.SOLO, TravelPace.BALANCED, Set.of(), List.of(place()), 100000,
                new BigDecimal("37.5"), new BigDecimal("127.0"), TransportMode.TRANSIT
        );
    }

    private PlaceResponse place() {
        return new PlaceResponse(
                1L, "장소", "SIGHT", "서울", null,
                new BigDecimal("37.6"), new BigDecimal("127.1"), null, null,
                LocalTime.of(9, 0), LocalTime.of(20, 0), 0, 60,
                "MANUAL", null, null, List.of()
        );
    }

    @FunctionalInterface
    private interface MatrixComputer {
        Map<RoutePair, RouteMetric> compute(List<RoutePoint> origins, List<RoutePoint> destinations);
    }
}
