package com.example.travelplanner.planning.route.google;

import com.example.travelplanner.planning.entity.TransportMode;
import com.example.travelplanner.planning.route.RouteMetric;
import com.example.travelplanner.planning.route.RoutePair;
import com.example.travelplanner.planning.route.RoutePoint;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class GoogleRoutesProviderTest {
    @Autowired
    private ObjectMapper objectMapper;
    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("Routes 실패를 재시도하고 거리와 시간을 내부 경로 형식으로 변환한다")
    void compute_retriesAndMapsMatrix() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/distanceMatrix/v2:computeRouteMatrix", exchange -> {
            assertEquals("test-key", exchange.getRequestHeaders().getFirst("X-Goog-Api-Key"));
            assertTrue(exchange.getRequestHeaders().getFirst("X-Goog-FieldMask").contains("duration"));
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(body.contains("\"travelMode\":\"DRIVE\""));
            if (calls.incrementAndGet() == 1) {
                respond(exchange, 500, "{}");
                return;
            }
            respond(exchange, 200, """
                    [{"originIndex":0,"destinationIndex":0,"status":{"code":0},
                      "condition":"ROUTE_EXISTS","distanceMeters":1250,"duration":"601.5s"}]
                    """);
        });
        server.start();

        GoogleRoutesProvider provider = new GoogleRoutesProvider(properties(), objectMapper);
        Map<RoutePair, RouteMetric> matrix = provider.compute(
                List.of(point("origin", "37.5", "127.0")),
                List.of(point("destination", "37.6", "127.1")),
                TransportMode.DRIVE
        );

        assertEquals(2, calls.get());
        assertEquals(11, matrix.get(new RoutePair("origin", "destination")).durationMinutes());
        assertEquals(1.25, matrix.get(new RoutePair("origin", "destination")).distanceKm());
    }

    @Test
    @DisplayName("대중교통 행렬은 요청당 100요소를 넘지 않도록 출발지를 분할한다")
    void compute_splitsTransitMatrixAtOneHundredElements() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/distanceMatrix/v2:computeRouteMatrix", exchange -> {
            calls.incrementAndGet();
            JsonNode body = objectMapper.readTree(exchange.getRequestBody());
            assertTrue(body.get("origins").size() * body.get("destinations").size() <= 100);
            respond(exchange, 200, """
                    [{"originIndex":0,"destinationIndex":0,"status":{"code":0},
                      "condition":"ROUTE_EXISTS","distanceMeters":1000,"duration":"600s"}]
                    """);
        });
        server.start();
        List<RoutePoint> origins = new ArrayList<>();
        List<RoutePoint> destinations = new ArrayList<>();
        for (int index = 0; index < 11; index++) {
            origins.add(point("o" + index, "37.5", "127.0"));
        }
        for (int index = 0; index < 10; index++) {
            destinations.add(point("d" + index, "37.6", "127.1"));
        }

        Map<RoutePair, RouteMetric> matrix = new GoogleRoutesProvider(properties(), objectMapper)
                .compute(origins, destinations, TransportMode.TRANSIT);

        assertEquals(2, calls.get());
        assertTrue(matrix.containsKey(new RoutePair("o0", "d0")));
        assertTrue(matrix.containsKey(new RoutePair("o10", "d0")));
    }

    private GoogleRoutesProperties properties() {
        GoogleRoutesProperties properties = new GoogleRoutesProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.setMaxRetries(1);
        properties.setRetryBackoffMillis(1);
        return properties;
    }

    private RoutePoint point(String key, String latitude, String longitude) {
        return new RoutePoint(key, new BigDecimal(latitude), new BigDecimal(longitude));
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
