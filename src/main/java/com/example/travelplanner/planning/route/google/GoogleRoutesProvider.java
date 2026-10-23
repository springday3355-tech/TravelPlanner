package com.example.travelplanner.planning.route.google;

import com.example.travelplanner.planning.entity.TransportMode;
import com.example.travelplanner.planning.route.RouteMatrixProvider;
import com.example.travelplanner.planning.route.RouteMetric;
import com.example.travelplanner.planning.route.RoutePair;
import com.example.travelplanner.planning.route.RoutePoint;
import com.example.travelplanner.planning.route.RouteProviderException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleRoutesProvider implements RouteMatrixProvider {
    private static final String FIELD_MASK =
            "originIndex,destinationIndex,status,condition,distanceMeters,duration";

    private final GoogleRoutesProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GoogleRoutesProvider(GoogleRoutesProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis()))
                .build());
    }

    GoogleRoutesProvider(
            GoogleRoutesProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Map<RoutePair, RouteMetric> compute(
            List<RoutePoint> origins,
            List<RoutePoint> destinations,
            TransportMode transportMode
    ) {
        if (origins.isEmpty() || destinations.isEmpty()) {
            return Map.of();
        }
        int requestLimit = transportMode == TransportMode.TRANSIT ? 100 : 625;
        int originsPerRequest = Math.max(1, requestLimit / destinations.size());
        Map<RoutePair, RouteMetric> result = new HashMap<>();
        for (int offset = 0; offset < origins.size(); offset += originsPerRequest) {
            int end = Math.min(offset + originsPerRequest, origins.size());
            List<RoutePoint> originBatch = origins.subList(offset, end);
            GoogleRouteElement[] elements = send(new GoogleRouteMatrixRequest(
                    toWaypoints(originBatch), toWaypoints(destinations), transportMode.name()
            ));
            for (GoogleRouteElement element : elements) {
                int originIndex = offset + element.originIndex();
                if (originIndex >= origins.size() || element.destinationIndex() >= destinations.size()
                        || !isSuccessful(element)) {
                    continue;
                }
                result.put(
                        new RoutePair(origins.get(originIndex).key(), destinations.get(element.destinationIndex()).key()),
                        new RouteMetric(toMinutes(element.duration()), toKilometres(element.distanceMeters()))
                );
            }
        }
        return Map.copyOf(result);
    }

    private GoogleRouteElement[] send(GoogleRouteMatrixRequest body) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(properties.getBaseUrl()
                                + "/distanceMatrix/v2:computeRouteMatrix"))
                        .timeout(Duration.ofMillis(properties.getRequestTimeoutMillis()))
                        .header("X-Goog-Api-Key", properties.getApiKey())
                        .header("X-Goog-FieldMask", FIELD_MASK)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                        .build();
                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return objectMapper.readValue(response.body(), GoogleRouteElement[].class);
                }
                if (isRetryable(response.statusCode()) && attempt <= properties.getMaxRetries()) {
                    backoff(attempt);
                    continue;
                }
                throw new RouteProviderException(
                        "Google Routes 요청 실패. 상태 코드: " + response.statusCode()
                );
            } catch (RouteProviderException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RouteProviderException("Google Routes 요청이 중단되었습니다.", e);
            } catch (IOException | RuntimeException e) {
                if (attempt <= properties.getMaxRetries()) {
                    backoff(attempt);
                    continue;
                }
                throw new RouteProviderException("Google Routes 서비스에 연결할 수 없습니다.", e);
            }
        }
    }

    private List<GoogleRouteMatrixWaypoint> toWaypoints(List<RoutePoint> points) {
        List<GoogleRouteMatrixWaypoint> result = new ArrayList<>();
        for (RoutePoint point : points) {
            result.add(new GoogleRouteMatrixWaypoint(new GoogleWaypoint(
                    new GoogleLocation(new GoogleLatLng(point.latitude(), point.longitude()))
            )));
        }
        return result;
    }

    private boolean isSuccessful(GoogleRouteElement element) {
        return (element.status() == null || element.status().code() == 0)
                && "ROUTE_EXISTS".equals(element.condition())
                && element.duration() != null
                && element.distanceMeters() != null;
    }

    private int toMinutes(String duration) {
        BigDecimal seconds = new BigDecimal(duration.substring(0, duration.length() - 1));
        return seconds.divide(BigDecimal.valueOf(60), 0, RoundingMode.CEILING).intValue();
    }

    private double toKilometres(Integer metres) {
        return BigDecimal.valueOf(metres)
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private boolean isRetryable(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(properties.getRetryBackoffMillis() * (1L << Math.min(attempt - 1, 5)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RouteProviderException("Google Routes 재시도가 중단되었습니다.", e);
        }
    }

    private record GoogleRouteMatrixRequest(
            List<GoogleRouteMatrixWaypoint> origins,
            List<GoogleRouteMatrixWaypoint> destinations,
            String travelMode
    ) {
    }

    private record GoogleRouteMatrixWaypoint(GoogleWaypoint waypoint) {
    }

    private record GoogleWaypoint(GoogleLocation location) {
    }

    private record GoogleLocation(GoogleLatLng latLng) {
    }

    private record GoogleLatLng(BigDecimal latitude, BigDecimal longitude) {
    }

    private record GoogleRouteElement(
            int originIndex,
            int destinationIndex,
            GoogleStatus status,
            String condition,
            Integer distanceMeters,
            String duration
    ) {
    }

    private record GoogleStatus(int code, String message) {
    }
}
