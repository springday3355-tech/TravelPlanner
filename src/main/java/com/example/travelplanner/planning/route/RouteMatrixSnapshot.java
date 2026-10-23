package com.example.travelplanner.planning.route;

import java.util.Map;
import java.util.Optional;

public record RouteMatrixSnapshot(
        RouteMatrixStatus status,
        Map<RoutePair, RouteMetric> metrics,
        int requestedElements
) {
    public RouteMatrixSnapshot {
        metrics = Map.copyOf(metrics);
    }

    public Optional<RouteMetric> find(String originKey, String destinationKey) {
        if (originKey == null || destinationKey == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(metrics.get(new RoutePair(originKey, destinationKey)));
    }

    public boolean usesExternalRoutes() {
        return status == RouteMatrixStatus.EXTERNAL || status == RouteMatrixStatus.CACHE;
    }
}
