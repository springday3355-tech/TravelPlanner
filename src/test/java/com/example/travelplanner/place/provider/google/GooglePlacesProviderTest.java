package com.example.travelplanner.place.provider.google;

import com.example.travelplanner.place.entity.PlaceCategory;
import com.example.travelplanner.place.provider.PlaceSearchCriteria;
import com.example.travelplanner.place.provider.ProviderPlace;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class GooglePlacesProviderTest {

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
    @DisplayName("Text Search 실패를 재시도하고 Place Details를 내부 장소 형식으로 변환한다")
    void search_retriesAndMapsDetails() {
        AtomicInteger searchCalls = new AtomicInteger();
        server.createContext("/v1/places:searchText", exchange -> {
            assertEquals("test-api-key", exchange.getRequestHeaders().getFirst("X-Goog-Api-Key"));
            assertEquals("places.id", exchange.getRequestHeaders().getFirst("X-Goog-FieldMask"));
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(requestBody.contains("서울 관광 명소"));
            assertTrue(requestBody.contains("\"pageSize\":5"));
            if (searchCalls.incrementAndGet() == 1) {
                respond(exchange, 500, "{}");
            } else {
                respond(exchange, 200, "{\"places\":[{\"id\":\"google-place-1\"}]}");
            }
        });
        server.createContext("/v1/places/google-place-1", exchange -> {
            assertTrue(exchange.getRequestHeaders().getFirst("X-Goog-FieldMask").contains("regularOpeningHours"));
            respond(exchange, 200, """
                    {
                      "id": "google-place-1",
                      "displayName": {"text": "테스트 박물관", "languageCode": "ko"},
                      "formattedAddress": "서울특별시 중구 테스트로 1",
                      "location": {"latitude": 37.5665, "longitude": 126.9780},
                      "primaryType": "museum",
                      "types": ["museum", "tourist_attraction"],
                      "priceLevel": "PRICE_LEVEL_MODERATE",
                      "regularOpeningHours": {
                        "weekdayDescriptions": ["월요일: 오전 9:00~오후 6:00"],
                        "periods": [
                          {"open": {"day": 1, "hour": 9, "minute": 0},
                           "close": {"day": 1, "hour": 18, "minute": 0}}
                        ]
                      }
                    }
                    """);
        });
        server.start();

        GooglePlacesProvider provider = new GooglePlacesProvider(properties(), objectMapper);
        List<ProviderPlace> places = provider.search(new PlaceSearchCriteria(
                "관광 명소", "서울", Set.of(PlaceCategory.SIGHT), 5
        ));

        assertEquals(2, searchCalls.get());
        assertEquals(1, places.size());
        ProviderPlace place = places.getFirst();
        assertEquals("google-place-1", place.externalId());
        assertEquals("테스트 박물관", place.name());
        assertEquals(PlaceCategory.SIGHT, place.category());
        assertEquals(DayOfWeek.MONDAY, place.openingHours().getFirst().dayOfWeek());
        assertEquals("09:00", place.openingHours().getFirst().openingTime().toString());
    }

    private GooglePlacesProperties properties() {
        GooglePlacesProperties properties = new GooglePlacesProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-api-key");
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.setMaxResults(10);
        properties.setMaxRetries(1);
        properties.setRetryBackoffMillis(1);
        return properties;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
