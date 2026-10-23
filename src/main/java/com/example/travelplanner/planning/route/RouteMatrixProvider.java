package com.example.travelplanner.planning.route;

import com.example.travelplanner.planning.entity.TransportMode;

import java.util.List;
import java.util.Map;

public interface RouteMatrixProvider {
    boolean isAvailable();

    Map<RoutePair, RouteMetric> compute(
            List<RoutePoint> origins,
            List<RoutePoint> destinations,
            TransportMode transportMode
    );
}
