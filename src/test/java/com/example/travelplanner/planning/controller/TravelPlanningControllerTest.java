package com.example.travelplanner.planning.controller;

import com.example.travelplanner.support.AuthTestHelper;
import com.example.travelplanner.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TravelPlanningControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("여행 조건을 입력하면 필수 장소와 취향을 우선해 날짜별 일정 초안을 저장한다")
    void generate_createsPersistedDraft() throws Exception {
        String token = signup("planner-success@example.com");
        createPlace(token, "계획테스트 전망대", "SIGHT", "계획도시A");
        long foodId = createPlace(token, "계획테스트 식당", "FOOD", "계획도시A");
        long cafeId = createPlace(token, "계획테스트 카페", "CAFE", "계획도시A");
        createPlace(token, "계획테스트 박물관", "SIGHT", "계획도시A");

        LocalDate startDate = LocalDate.now().plusDays(20);
        Map<String, Object> request = validRequest("계획도시A", startDate, startDate.plusDays(1));
        request.put("preferredCategories", List.of("FOOD"));
        request.put("dailyStartTime", "09:00");
        request.put("dailyEndTime", "14:00");
        request.put("requiredPlaceIds", List.of(cafeId));
        request.put("excludedPlaceIds", List.of());

        MvcResult result = mockMvc.perform(post("/api/travel-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.generator").value("ROUTE_AWARE_V2"))
                .andExpect(jsonPath("$.data.selectedPlaceCount").value(4))
                .andExpect(jsonPath("$.data.availableSlotCount").value(8))
                .andExpect(jsonPath("$.data.itinerary.days.length()").value(2))
                .andExpect(jsonPath("$.data.itinerary.days[0].items.length()").value(3))
                .andExpect(jsonPath("$.data.itinerary.days[0].items[0].placeId").value(cafeId))
                .andExpect(jsonPath("$.data.itinerary.days[0].items[1].placeId").value(foodId))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        long itineraryId = response.get("data").get("itinerary").get("id").asLong();
        mockMvc.perform(get("/api/itineraries/" + itineraryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[0].items[0].placeId").value(cafeId))
                .andExpect(jsonPath("$.data.days[1].items.length()").value(1));
    }

    @Test
    @DisplayName("여행 도시에 후보 장소가 없으면 빈 일정을 만들지 않고 G001을 반환한다")
    void generate_withoutCandidates_isRejected() throws Exception {
        String token = signup("planner-empty@example.com");
        LocalDate startDate = LocalDate.now().plusDays(30);

        mockMvc.perform(post("/api/travel-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(
                                "등록장소없는도시", startDate, startDate.plusDays(1)
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("G001"));

        mockMvc.perform(get("/api/itineraries").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("여행 속도 기준 용량보다 필수 장소가 많으면 G003을 반환하고 일정은 저장하지 않는다")
    void generate_withTooManyRequiredPlaces_isRejected() throws Exception {
        String token = signup("planner-capacity@example.com");
        long first = createPlace(token, "필수장소1", "SIGHT", "계획도시B");
        long second = createPlace(token, "필수장소2", "CAFE", "계획도시B");
        long third = createPlace(token, "필수장소3", "FOOD", "계획도시B");
        long fourth = createPlace(token, "필수장소4", "SIGHT", "계획도시B");
        LocalDate startDate = LocalDate.now().plusDays(40);
        Map<String, Object> request = validRequest("계획도시B", startDate, startDate);
        request.put("travelPace", "RELAXED");
        request.put("requiredPlaceIds", List.of(first, second, third, fourth));

        mockMvc.perform(post("/api/travel-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("G003"));

        mockMvc.perform(get("/api/itineraries").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("필수 장소가 영업시간 안에 배치되지 않으면 G004를 반환한다")
    void generate_withClosedRequiredPlace_isRejected() throws Exception {
        String token = signup("planner-hours@example.com");
        long closedPlace = createPlanningPlace(
                token, "저녁 전용 장소", "SIGHT", "계획도시D",
                "37.5000", "127.0000", "18:00", "21:00", 20000, 60
        );
        LocalDate startDate = LocalDate.now().plusDays(60);
        Map<String, Object> request = validRequest("계획도시D", startDate, startDate);
        request.put("dailyStartTime", "09:00");
        request.put("dailyEndTime", "17:00");
        request.put("requiredPlaceIds", List.of(closedPlace));

        mockMvc.perform(post("/api/travel-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("G004"));
    }

    @Test
    @DisplayName("장소가 늦게 열면 방문 시작 시간을 영업 시작 시각으로 조정한다")
    void generate_waitsUntilOpeningTime() throws Exception {
        String token = signup("planner-opening@example.com");
        long placeId = createPlanningPlace(
                token, "정오 개장 장소", "SIGHT", "계획도시G",
                "37.5000", "127.0000", "12:00", "18:00", 10000, 60
        );
        LocalDate startDate = LocalDate.now().plusDays(90);
        Map<String, Object> request = validRequest("계획도시G", startDate, startDate);

        mockMvc.perform(post("/api/travel-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.itinerary.days[0].items[0].placeId").value(placeId))
                .andExpect(jsonPath("$.data.itinerary.days[0].items[0].startTime").value("12:00:00"))
                .andExpect(jsonPath("$.data.itinerary.days[0].items[0].endTime").value("13:00:00"));
    }

    @Test
    @DisplayName("출발 좌표에서 가까우면서 바로 방문 가능한 장소부터 동선을 구성한다")
    void generate_ordersPlacesByAvailabilityAndDistance() throws Exception {
        String token = signup("planner-route@example.com");
        long far = createPlanningPlace(
                token, "먼 장소", "SIGHT", "계획도시E",
                "37.6000", "127.1000", "09:00", "20:00", 10000, 60
        );
        long near = createPlanningPlace(
                token, "가까운 장소", "SIGHT", "계획도시E",
                "37.5010", "127.0010", "09:00", "20:00", 10000, 60
        );
        long middle = createPlanningPlace(
                token, "중간 장소", "SIGHT", "계획도시E",
                "37.5100", "127.0100", "09:00", "20:00", 10000, 60
        );
        LocalDate startDate = LocalDate.now().plusDays(70);
        Map<String, Object> request = validRequest("계획도시E", startDate, startDate);
        request.put("travelPace", "ACTIVE");
        request.put("startLatitude", "37.5000");
        request.put("startLongitude", "127.0000");

        mockMvc.perform(post("/api/travel-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.generator").value("ROUTE_AWARE_V2"))
                .andExpect(jsonPath("$.data.selectedPlaceCount").value(3))
                .andExpect(jsonPath("$.data.estimatedCost").value(30000))
                .andExpect(jsonPath("$.data.estimatedTravelMinutes").isNumber())
                .andExpect(jsonPath("$.data.estimatedDistanceKm").isNumber())
                .andExpect(jsonPath("$.data.itinerary.days[0].items[0].placeId").value(near))
                .andExpect(jsonPath("$.data.itinerary.days[0].items[1].placeId").value(middle))
                .andExpect(jsonPath("$.data.itinerary.days[0].items[2].placeId").value(far));
    }

    @Test
    @DisplayName("예산을 초과하는 선택 장소는 제외하지만 필수 장소는 유지한다")
    void generate_respectsBudgetExceptForRequiredPlace() throws Exception {
        String token = signup("planner-budget@example.com");
        long expensive = createPlanningPlace(
                token, "비싼 필수 장소", "SIGHT", "계획도시F",
                "37.5010", "127.0010", "09:00", "20:00", 100000, 60
        );
        createPlanningPlace(
                token, "예산 내 장소", "SIGHT", "계획도시F",
                "37.5100", "127.0100", "09:00", "20:00", 50000, 60
        );
        LocalDate startDate = LocalDate.now().plusDays(80);
        Map<String, Object> request = validRequest("계획도시F", startDate, startDate);
        request.put("budget", 60000);
        request.put("requiredPlaceIds", List.of(expensive));
        request.put("startLatitude", "37.5000");
        request.put("startLongitude", "127.0000");

        mockMvc.perform(post("/api/travel-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.selectedPlaceCount").value(1))
                .andExpect(jsonPath("$.data.estimatedCost").value(100000))
                .andExpect(jsonPath("$.data.itinerary.days[0].items[0].placeId").value(expensive))
                .andExpect(jsonPath("$.data.itinerary.days[0].items[0].memo").value(
                        org.hamcrest.Matchers.containsString("예상 100000원")
                ))
                .andExpect(jsonPath("$.data.warnings[*]").value(
                        org.hamcrest.Matchers.hasItem("필수 장소를 포함하기 위해 예상 비용이 입력 예산을 초과했습니다.")
                ));
    }

    @Test
    @DisplayName("다른 도시의 장소를 필수 장소로 지정하면 G002를 반환한다")
    void generate_withRequiredPlaceInAnotherCity_isRejected() throws Exception {
        String token = signup("planner-region@example.com");
        long otherCityPlace = createPlace(token, "타도시장소", "SIGHT", "다른계획도시");
        createPlace(token, "여행도시장소", "SIGHT", "계획도시C");
        LocalDate startDate = LocalDate.now().plusDays(50);
        Map<String, Object> request = validRequest("계획도시C", startDate, startDate);
        request.put("requiredPlaceIds", List.of(otherCityPlace));

        mockMvc.perform(post("/api/travel-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("G002"));
    }

    private String signup(String email) throws Exception {
        return AuthTestHelper.signupAndGetToken(mockMvc, objectMapper, email, "password123", "여행계획자");
    }

    private long createPlace(String token, String name, String category, String region) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", name, "category", category, "region", region
        ));
        MvcResult result = mockMvc.perform(post("/api/places")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("data").get("id").asLong();
    }

    private long createPlanningPlace(
            String token, String name, String category, String region,
            String latitude, String longitude, String openingTime, String closingTime,
            int estimatedCost, int recommendedVisitMinutes
    ) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("category", category);
        body.put("region", region);
        body.put("latitude", latitude);
        body.put("longitude", longitude);
        body.put("openingTime", openingTime);
        body.put("closingTime", closingTime);
        body.put("estimatedCost", estimatedCost);
        body.put("recommendedVisitMinutes", recommendedVisitMinutes);
        MvcResult result = mockMvc.perform(post("/api/places")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("data").get("id").asLong();
    }

    private Map<String, Object> validRequest(String city, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", city + " 자동 일정");
        request.put("city", city);
        request.put("startDate", startDate.toString());
        request.put("endDate", endDate.toString());
        request.put("budget", 300000);
        request.put("preferredCategories", List.of("SIGHT", "FOOD"));
        request.put("companionType", "FRIENDS");
        request.put("travelPace", "BALANCED");
        request.put("dailyStartTime", "09:00");
        request.put("dailyEndTime", "18:00");
        request.put("requiredPlaceIds", List.of());
        request.put("excludedPlaceIds", List.of());
        return request;
    }
}
