package com.example.travelplanner.place.controller;

import com.example.travelplanner.support.AuthTestHelper;
import com.example.travelplanner.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.LinkedHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaceControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("인증 없이도 장소 목록을 조회할 수 있다")
    void search_withoutAuth_succeeds() throws Exception {
        mockMvc.perform(get("/api/places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("인증 없이 장소를 등록하면 401을 반환한다")
    void create_withoutAuth_isUnauthorized() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("name", "해운대", "category", "SIGHT", "region", "부산")
        );
        mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증된 사용자는 장소를 등록하고 지역으로 필터링해 조회할 수 있다")
    void create_and_filter_byRegion() throws Exception {
        String token = AuthTestHelper.signupAndGetToken(
                mockMvc, objectMapper, "placeowner@example.com", "password123", "장소관리자"
        );

        String body = objectMapper.writeValueAsString(
                Map.of("name", "해운대 해수욕장", "category", "SIGHT", "region", "부산")
        );

        mockMvc.perform(post("/api/places")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("해운대 해수욕장"));

        mockMvc.perform(get("/api/places").param("region", "부산"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].region").value("부산"));
    }

    @Test
    @DisplayName("장소의 좌표와 영업시간은 각각 한 쌍으로 입력해야 한다")
    void create_withIncompletePlanningFields_isRejected() throws Exception {
        String token = AuthTestHelper.signupAndGetToken(
                mockMvc, objectMapper, "place-validation@example.com", "password123", "장소검증"
        );
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "잘못된 장소");
        body.put("category", "SIGHT");
        body.put("region", "서울");
        body.put("latitude", 37.5);
        body.put("openingTime", "09:00");

        mockMvc.perform(post("/api/places")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("일정에서 사용 중인 장소는 삭제할 수 없다")
    void delete_inUsePlace_isRejected() throws Exception {
        String token = AuthTestHelper.signupAndGetToken(
                mockMvc, objectMapper, "place-usage@example.com", "password123", "참조검증"
        );

        String placeBody = objectMapper.writeValueAsString(
                Map.of("name", "참조 장소", "category", "SIGHT", "region", "서울")
        );
        MvcResult placeResult = mockMvc.perform(post("/api/places")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeBody))
                .andExpect(status().isCreated())
                .andReturn();
        long placeId = objectMapper.readTree(placeResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("data").get("id").asLong();

        LocalDate startDate = LocalDate.now().plusDays(50);
        String itineraryBody = objectMapper.writeValueAsString(Map.of(
                "title", "장소 참조 일정", "city", "서울",
                "startDate", startDate.toString(), "endDate", startDate.plusDays(1).toString(),
                "budget", 100000
        ));
        MvcResult itineraryResult = mockMvc.perform(post("/api/itineraries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itineraryBody))
                .andExpect(status().isCreated())
                .andReturn();
        long itineraryId = objectMapper.readTree(itineraryResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("data").get("id").asLong();

        String dayBody = objectMapper.writeValueAsString(Map.of(
                "dayNumber", 1, "date", startDate.toString()
        ));
        MvcResult dayResult = mockMvc.perform(post("/api/itineraries/" + itineraryId + "/days")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dayBody))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode dayJson = objectMapper.readTree(dayResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        long dayId = dayJson.get("data").get("id").asLong();

        String itemBody = objectMapper.writeValueAsString(Map.of(
                "placeId", placeId,
                "visitOrder", 1,
                "startTime", "10:00",
                "endTime", "11:00"
        ));
        mockMvc.perform(post("/api/itineraries/" + itineraryId + "/days/" + dayId + "/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemBody))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/places/" + placeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("L002"));
    }
}
