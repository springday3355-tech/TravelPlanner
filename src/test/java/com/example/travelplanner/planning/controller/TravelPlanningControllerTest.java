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
                .andExpect(jsonPath("$.data.generator").value("RULE_BASED_V1"))
                .andExpect(jsonPath("$.data.selectedPlaceCount").value(4))
                .andExpect(jsonPath("$.data.availableSlotCount").value(4))
                .andExpect(jsonPath("$.data.itinerary.days.length()").value(2))
                .andExpect(jsonPath("$.data.itinerary.days[0].items.length()").value(2))
                .andExpect(jsonPath("$.data.itinerary.days[0].items[0].placeId").value(cafeId))
                .andExpect(jsonPath("$.data.itinerary.days[0].items[1].placeId").value(foodId))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        long itineraryId = response.get("data").get("itinerary").get("id").asLong();
        mockMvc.perform(get("/api/itineraries/" + itineraryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[0].items[0].placeId").value(cafeId))
                .andExpect(jsonPath("$.data.days[1].items.length()").value(2));
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
    @DisplayName("일정 용량보다 필수 장소가 많으면 G003을 반환하고 일정은 저장하지 않는다")
    void generate_withTooManyRequiredPlaces_isRejected() throws Exception {
        String token = signup("planner-capacity@example.com");
        long first = createPlace(token, "필수장소1", "SIGHT", "계획도시B");
        long second = createPlace(token, "필수장소2", "CAFE", "계획도시B");
        LocalDate startDate = LocalDate.now().plusDays(40);
        Map<String, Object> request = validRequest("계획도시B", startDate, startDate);
        request.put("dailyStartTime", "10:00");
        request.put("dailyEndTime", "11:30");
        request.put("requiredPlaceIds", List.of(first, second));

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
