package com.example.travelplanner.place.provider.google;

import com.example.travelplanner.common.exception.BusinessException;
import com.example.travelplanner.common.exception.ErrorCode;
import com.example.travelplanner.place.entity.PlaceCategory;
import com.example.travelplanner.place.entity.PlaceSource;
import com.example.travelplanner.place.provider.PlaceProvider;
import com.example.travelplanner.place.provider.PlaceSearchCriteria;
import com.example.travelplanner.place.provider.ProviderOpeningHour;
import com.example.travelplanner.place.provider.ProviderPlace;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GooglePlacesProvider implements PlaceProvider {

    private static final String TEXT_SEARCH_FIELDS = "places.id";
    private static final String DETAIL_FIELDS = String.join(",",
            "id", "displayName", "formattedAddress", "location", "primaryType", "types",
            "regularOpeningHours", "priceLevel", "priceRange"
    );

    private final GooglePlacesProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GooglePlacesProvider(GooglePlacesProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis()))
                .build());
    }

    GooglePlacesProvider(GooglePlacesProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public PlaceSource source() {
        return PlaceSource.GOOGLE_PLACES;
    }

    @Override
    public List<ProviderPlace> search(PlaceSearchCriteria criteria) {
        int limit = Math.min(criteria.limit(), properties.getMaxResults());
        GoogleTextSearchRequest requestBody = new GoogleTextSearchRequest(
                criteria.region() + " " + criteria.query(), "ko", limit
        );
        GoogleTextSearchResponse searchResponse = send(
                "/v1/places:searchText", "POST", requestBody, TEXT_SEARCH_FIELDS,
                GoogleTextSearchResponse.class
        );
        if (searchResponse.places() == null) {
            return List.of();
        }
        return searchResponse.places().stream()
                .filter(place -> place.id() != null)
                .limit(limit)
                .map(place -> getDetails(place.id(), criteria.region()))
                .filter(place -> criteria.categories() == null || criteria.categories().isEmpty()
                        || criteria.categories().contains(place.category()))
                .toList();
    }

    private ProviderPlace getDetails(String placeId, String region) {
        String encodedId = URLEncoder.encode(placeId, StandardCharsets.UTF_8);
        GooglePlace place = send(
                "/v1/places/" + encodedId + "?languageCode=ko",
                "GET", null, DETAIL_FIELDS, GooglePlace.class
        );
        PlaceCategory category = mapCategory(place.primaryType(), place.types());
        return new ProviderPlace(
                place.id(), textOf(place.displayName()), category, region, place.formattedAddress(),
                latitudeOf(place.location()), longitudeOf(place.location()), formatPrice(place),
                formatOpeningHours(place.regularOpeningHours()), null, recommendedVisitMinutes(category),
                mapOpeningHours(place.regularOpeningHours())
        );
    }

    private <T> T send(String path, String method, Object body, String fieldMask, Class<T> responseType) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(properties.getBaseUrl() + path))
                        .timeout(Duration.ofMillis(properties.getRequestTimeoutMillis()))
                        .header("X-Goog-Api-Key", properties.getApiKey())
                        .header("X-Goog-FieldMask", fieldMask)
                        .header("Content-Type", "application/json");
                if ("POST".equals(method)) {
                    builder.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
                } else {
                    builder.GET();
                }
                HttpResponse<String> response = httpClient.send(
                        builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return objectMapper.readValue(response.body(), responseType);
                }
                if (isRetryable(response.statusCode()) && attempt <= properties.getMaxRetries()) {
                    backoff(attempt);
                    continue;
                }
                throw new BusinessException(
                        ErrorCode.PLACE_PROVIDER_FAILED,
                        "외부 장소 서비스가 요청을 처리하지 못했습니다. 상태 코드: " + response.statusCode()
                );
            } catch (BusinessException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.PLACE_PROVIDER_FAILED, "외부 장소 조회가 중단되었습니다.");
            } catch (IOException | RuntimeException e) {
                if (attempt <= properties.getMaxRetries()) {
                    backoff(attempt);
                    continue;
                }
                throw new BusinessException(ErrorCode.PLACE_PROVIDER_FAILED, "외부 장소 서비스에 연결할 수 없습니다.");
            }
        }
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(properties.getRetryBackoffMillis() * (1L << Math.min(attempt - 1, 5)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.PLACE_PROVIDER_FAILED, "외부 장소 조회 재시도가 중단되었습니다.");
        }
    }

    private boolean isRetryable(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private String textOf(GoogleLocalizedText displayName) {
        return displayName == null ? null : displayName.text();
    }

    private BigDecimal latitudeOf(GoogleLocation location) {
        return location == null || location.latitude() == null ? null : BigDecimal.valueOf(location.latitude());
    }

    private BigDecimal longitudeOf(GoogleLocation location) {
        return location == null || location.longitude() == null ? null : BigDecimal.valueOf(location.longitude());
    }

    private PlaceCategory mapCategory(String primaryType, List<String> types) {
        Set<String> allTypes = new java.util.HashSet<>();
        if (primaryType != null) {
            allTypes.add(primaryType.toLowerCase(Locale.ROOT));
        }
        if (types != null) {
            types.stream().map(type -> type.toLowerCase(Locale.ROOT)).forEach(allTypes::add);
        }
        if (containsAny(allTypes, "cafe", "coffee_shop", "bakery")) {
            return PlaceCategory.CAFE;
        }
        if (containsAny(allTypes, "restaurant", "meal_takeaway", "meal_delivery", "food")) {
            return PlaceCategory.FOOD;
        }
        if (containsAny(allTypes, "lodging", "hotel", "motel", "hostel", "resort_hotel", "guest_house")) {
            return PlaceCategory.ACCOMMODATION;
        }
        return PlaceCategory.SIGHT;
    }

    private boolean containsAny(Set<String> values, String... candidates) {
        return Arrays.stream(candidates).anyMatch(values::contains);
    }

    private int recommendedVisitMinutes(PlaceCategory category) {
        return switch (category) {
            case FOOD -> 90;
            case CAFE -> 60;
            case SIGHT -> 90;
            case ACCOMMODATION -> 60;
        };
    }

    private String formatPrice(GooglePlace place) {
        if (place.priceRange() != null) {
            String start = formatMoney(place.priceRange().startPrice());
            String end = formatMoney(place.priceRange().endPrice());
            if (start != null && end != null) {
                return start + "~" + end;
            }
        }
        return place.priceLevel();
    }

    private String formatMoney(GoogleMoney money) {
        if (money == null || money.currencyCode() == null || money.units() == null) {
            return null;
        }
        return money.currencyCode() + " " + money.units();
    }

    private String formatOpeningHours(GoogleOpeningHours openingHours) {
        if (openingHours == null || openingHours.weekdayDescriptions() == null) {
            return null;
        }
        return String.join(" | ", openingHours.weekdayDescriptions());
    }

    private List<ProviderOpeningHour> mapOpeningHours(GoogleOpeningHours openingHours) {
        if (openingHours == null || openingHours.periods() == null) {
            return List.of();
        }
        List<ProviderOpeningHour> result = new ArrayList<>();
        for (GooglePeriod period : openingHours.periods()) {
            if (period.open() == null) {
                continue;
            }
            if (period.close() == null || sameMoment(period.open(), period.close())) {
                Arrays.stream(DayOfWeek.values()).forEach(day -> result.add(
                        new ProviderOpeningHour(day, LocalTime.MIN, LocalTime.of(23, 59), false)
                ));
                continue;
            }
            LocalTime openingTime = toLocalTime(period.open());
            LocalTime closingTime = toLocalTime(period.close());
            boolean closesNextDay = period.close().day() != period.open().day()
                    || !closingTime.isAfter(openingTime);
            result.add(new ProviderOpeningHour(
                    toDayOfWeek(period.open().day()), openingTime, closingTime, closesNextDay
            ));
        }
        return List.copyOf(result);
    }

    private boolean sameMoment(GoogleTime first, GoogleTime second) {
        return first.day() == second.day()
                && valueOrZero(first.hour()) == valueOrZero(second.hour())
                && valueOrZero(first.minute()) == valueOrZero(second.minute());
    }

    private LocalTime toLocalTime(GoogleTime time) {
        return LocalTime.of(valueOrZero(time.hour()), valueOrZero(time.minute()));
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private DayOfWeek toDayOfWeek(int googleDay) {
        return DayOfWeek.of(googleDay == 0 ? 7 : googleDay);
    }

    private record GoogleTextSearchRequest(String textQuery, String languageCode, int pageSize) {
    }

    private record GoogleTextSearchResponse(List<GooglePlaceReference> places) {
    }

    private record GooglePlaceReference(String id) {
    }

    private record GooglePlace(
            String id,
            GoogleLocalizedText displayName,
            String formattedAddress,
            GoogleLocation location,
            String primaryType,
            List<String> types,
            GoogleOpeningHours regularOpeningHours,
            String priceLevel,
            GooglePriceRange priceRange
    ) {
    }

    private record GoogleLocalizedText(String text) {
    }

    private record GoogleLocation(Double latitude, Double longitude) {
    }

    private record GoogleOpeningHours(List<GooglePeriod> periods, List<String> weekdayDescriptions) {
    }

    private record GooglePeriod(GoogleTime open, GoogleTime close) {
    }

    private record GoogleTime(int day, Integer hour, Integer minute) {
    }

    private record GooglePriceRange(GoogleMoney startPrice, GoogleMoney endPrice) {
    }

    private record GoogleMoney(String currencyCode, String units, Integer nanos) {
    }
}
