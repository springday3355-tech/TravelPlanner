package com.example.travelplanner.planning.route;

import com.example.travelplanner.place.dto.PlaceResponse;
import com.example.travelplanner.planning.entity.TransportMode;
import com.example.travelplanner.planning.route.google.GoogleRoutesProperties;
import com.example.travelplanner.planning.service.PlanGenerationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RouteMatrixService {
    public static final String START_KEY = "START";

    private final RouteMatrixProvider provider;
    private final GoogleRoutesProperties properties;
    private final Clock clock = Clock.systemUTC();
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public RouteMatrixSnapshot resolve(PlanGenerationContext context) {
        List<RoutePoint> destinations = context.candidates().stream()
                .filter(this::hasCoordinates)
                .map(this::toPoint)
                .toList();
        if (destinations.isEmpty()) {
            return empty(RouteMatrixStatus.NO_COORDINATES, 0);
        }

        List<RoutePoint> origins = new ArrayList<>(destinations);
        if (context.startLatitude() != null && context.startLongitude() != null) {
            origins.add(0, new RoutePoint(START_KEY, context.startLatitude(), context.startLongitude()));
        }
        int requestedElements = origins.size() * destinations.size();
        if (!provider.isAvailable()) {
            return empty(RouteMatrixStatus.DISABLED, requestedElements);
        }
        if (requestedElements > properties.getMaxElementsPerPlan()) {
            return empty(RouteMatrixStatus.COST_LIMIT, requestedElements);
        }

        String cacheKey = cacheKey(origins, destinations, context.transportMode());
        CacheEntry cached = cache.get(cacheKey);
        Instant now = clock.instant();
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return new RouteMatrixSnapshot(RouteMatrixStatus.CACHE, cached.metrics(), requestedElements);
        }
        try {
            Map<RoutePair, RouteMetric> metrics = provider.compute(
                    origins, destinations, context.transportMode()
            );
            putCache(cacheKey, metrics, now);
            return new RouteMatrixSnapshot(RouteMatrixStatus.EXTERNAL, metrics, requestedElements);
        } catch (RuntimeException e) {
            return empty(RouteMatrixStatus.FAILED, requestedElements);
        }
    }

    private void putCache(String key, Map<RoutePair, RouteMetric> metrics, Instant now) {
        if (cache.size() >= properties.getCacheMaxEntries()) {
            cache.entrySet().stream()
                    .min(Comparator.comparing(entry -> entry.getValue().expiresAt()))
                    .ifPresent(entry -> cache.remove(entry.getKey(), entry.getValue()));
        }
        cache.put(key, new CacheEntry(
                Map.copyOf(metrics), now.plusSeconds(properties.getCacheTtlSeconds())
        ));
    }

    private String cacheKey(
            List<RoutePoint> origins,
            List<RoutePoint> destinations,
            TransportMode transportMode
    ) {
        return transportMode + "|" + pointKey(origins) + "|" + pointKey(destinations);
    }

    private String pointKey(List<RoutePoint> points) {
        return points.stream()
                .map(point -> point.key() + ":" + point.latitude().stripTrailingZeros()
                        + "," + point.longitude().stripTrailingZeros())
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
    }

    private RouteMatrixSnapshot empty(RouteMatrixStatus status, int requestedElements) {
        return new RouteMatrixSnapshot(status, Map.of(), requestedElements);
    }

    private boolean hasCoordinates(PlaceResponse place) {
        return place.latitude() != null && place.longitude() != null;
    }

    private RoutePoint toPoint(PlaceResponse place) {
        return new RoutePoint(place.id().toString(), place.latitude(), place.longitude());
    }

    private record CacheEntry(Map<RoutePair, RouteMetric> metrics, Instant expiresAt) {
    }
}
