package com.example.travelplanner.planning.route;

import com.example.travelplanner.planning.entity.TransportMode;

import java.util.List;
import java.util.Map;

public class UnavailableRouteMatrixProvider implements RouteMatrixProvider {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Map<RoutePair, RouteMetric> compute(
            List<RoutePoint> origins,
            List<RoutePoint> destinations,
            TransportMode transportMode
    ) {
        return Map.of();
    }
}
