package com.example.travelplanner.itinerary.controller;

import com.example.travelplanner.support.AuthTestHelper;
import com.example.travelplanner.support.ControllerTestSupport;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItineraryControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("일정을 생성하면 소유자(OWNER)로 참여자에 등록되고, 상세 조회가 가능하다")
    void create_and_getDetail() throws Exception {
        String token = AuthTestHelper.signupAndGetToken(
                mockMvc, objectMapper, "owner@example.com", "password123", "오너"
        );

        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "부산 2박 3일",
                "city", "부산",
                "startDate", LocalDate.now().plusDays(30).toString(),
                "endDate", LocalDate.now().plusDays(32).toString(),
                "budget", 500000
        ));

        MvcResult createResult = mockMvc.perform(post("/api/itineraries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.myRole").value("OWNER"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        long itineraryId = created.get("data").get("id").asLong();

        mockMvc.perform(get("/api/itineraries/" + itineraryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("부산 2박 3일"))
                .andExpect(jsonPath("$.data.participants[0].participantRole").value("OWNER"));

        mockMvc.perform(get("/api/itineraries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(itineraryId));
    }

    @Test
    @DisplayName("일정에 참여하지 않은 사용자가 조회하면 접근 거부(P002) 응답을 받는다")
    void getDetail_byNonParticipant_isDenied() throws Exception {
        String ownerToken = AuthTestHelper.signupAndGetToken(
                mockMvc, objectMapper, "owner2@example.com", "password123", "오너2"
        );
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "제주 1박 2일", "city", "제주",
                "startDate", LocalDate.now().plusDays(40).toString(),
                "endDate", LocalDate.now().plusDays(41).toString(),
                "budget", 300000
        ));
        MvcResult createResult = mockMvc.perform(post("/api/itineraries")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        long itineraryId = objectMapper.readTree(createResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("data").get("id").asLong();

        String strangerToken = AuthTestHelper.signupAndGetToken(
                mockMvc, objectMapper, "stranger@example.com", "password123", "타인"
        );

        mockMvc.perform(get("/api/itineraries/" + itineraryId)
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("P002"));
    }

    @Test
    @DisplayName("종료일이 시작일보다 빠른 일정은 생성할 수 없다")
    void create_withInvalidPeriod_isRejected() throws Exception {
        String token = AuthTestHelper.signupAndGetToken(
                mockMvc, objectMapper, "invalid-period@example.com", "password123", "기간검증"
        );
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "잘못된 일정",
                "city", "서울",
                "startDate", LocalDate.now().plusDays(20).toString(),
                "endDate", LocalDate.now().plusDays(10).toString(),
                "budget", 100000
        ));

        mockMvc.perform(post("/api/itineraries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("종료일은 시작일보다 빠를 수 없습니다."));
    }

    @Test
    @DisplayName("OWNER 역할로 초대하거나 기존 소유자를 동행자에서 제거할 수 없다")
    void ownerRole_isProtected() throws Exception {
        String ownerToken = AuthTestHelper.signupAndGetToken(
                mockMvc, objectMapper, "protected-owner@example.com", "password123", "보호소유자"
        );
        AuthTestHelper.signupAndGetToken(
                mockMvc, objectMapper, "new-owner@example.com", "password123", "새소유자"
        );

        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "소유권 보호 일정",
                "city", "서울",
                "startDate", LocalDate.now().plusDays(30).toString(),
                "endDate", LocalDate.now().plusDays(31).toString(),
                "budget", 200000
        ));
        MvcResult createResult = mockMvc.perform(post("/api/itineraries")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        long itineraryId = objectMapper.readTree(createResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("data").get("id").asLong();

        String inviteBody = objectMapper.writeValueAsString(Map.of(
                "memberEmail", "new-owner@example.com",
                "participantRole", "OWNER"
        ));
        mockMvc.perform(post("/api/itineraries/" + itineraryId + "/participants")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("P008"));

        MvcResult detailResult = mockMvc.perform(get("/api/itineraries/" + itineraryId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        long ownerParticipantId = objectMapper.readTree(detailResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("data").get("participants").get(0).get("id").asLong();

        mockMvc.perform(delete("/api/itineraries/" + itineraryId + "/participants/" + ownerParticipantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("P009"));
    }
}
