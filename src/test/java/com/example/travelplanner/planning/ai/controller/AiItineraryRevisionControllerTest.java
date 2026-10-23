package com.example.travelplanner.planning.ai.controller;

import com.example.travelplanner.itinerary.dto.ItemChangeOperation;
import com.example.travelplanner.itinerary.dto.ItineraryItemChangeCommand;
import com.example.travelplanner.planning.ai.provider.AiRevisionContext;
import com.example.travelplanner.planning.ai.provider.AiRevisionProposal;
import com.example.travelplanner.planning.ai.provider.ItineraryRevisionProvider;
import com.example.travelplanner.support.AuthTestHelper;
import com.example.travelplanner.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiItineraryRevisionControllerTest extends ControllerTestSupport {
    @MockitoBean
    private ItineraryRevisionProvider provider;

    @BeforeEach
    void setUpProvider() {
        reset(provider);
        when(provider.isAvailable()).thenReturn(true);
        when(provider.name()).thenReturn("TEST_AI");
        when(provider.model()).thenReturn("test-model");
        when(provider.promptVersion()).thenReturn("test-v1");
    }

    @Test
    @DisplayName("AI 제안을 검증한 뒤 장소와 방문 시간을 한 트랜잭션으로 변경한다")
    void revise_appliesValidatedUpdate() throws Exception {
        String token = signup("ai-owner-success@example.com");
        long originalPlaceId = createPlace(token, "야외 공원", "AI도시A", 10000, "09:00", "18:00");
        long indoorPlaceId = createPlace(token, "실내 박물관", "AI도시A", 15000, "09:00", "18:00");
        PlanIds plan = createPlan(token, "AI도시A", 50000, originalPlaceId);
        when(provider.propose(any(AiRevisionContext.class), eq("비가 오면 실내로 바꿔줘")))
                .thenReturn(new AiRevisionProposal(
                        "실내 박물관으로 변경했습니다.",
                        List.of(new ItineraryItemChangeCommand(
                                ItemChangeOperation.UPDATE, plan.dayId(), plan.itemId(), indoorPlaceId,
                                LocalTime.of(10, 0), LocalTime.of(11, 0), "AI가 변경", "우천 대응"
                        ))
                ));

        mockMvc.perform(post("/api/itineraries/" + plan.itineraryId() + "/ai-revisions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruction\":\"비가 오면 실내로 바꿔줘\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("TEST_AI"))
                .andExpect(jsonPath("$.data.model").value("test-model"))
                .andExpect(jsonPath("$.data.changes[0].beforePlaceId").value(originalPlaceId))
                .andExpect(jsonPath("$.data.changes[0].afterPlaceId").value(indoorPlaceId))
                .andExpect(jsonPath("$.data.itinerary.days[0].items[0].placeId").value(indoorPlaceId))
                .andExpect(jsonPath("$.data.itinerary.days[0].items[0].startTime").value("10:00:00"));
    }

    @Test
    @DisplayName("AI 변경이 예산을 초과하면 전체 변경을 거부하고 기존 일정을 보존한다")
    void revise_rejectsOverBudgetProposalWithoutMutation() throws Exception {
        String token = signup("ai-owner-budget@example.com");
        long originalPlaceId = createPlace(token, "기존 장소", "AI도시B", 10000, "09:00", "18:00");
        long expensivePlaceId = createPlace(token, "고가 장소", "AI도시B", 100000, "09:00", "18:00");
        PlanIds plan = createPlan(token, "AI도시B", 20000, originalPlaceId);
        when(provider.propose(any(AiRevisionContext.class), any(String.class)))
                .thenReturn(new AiRevisionProposal(
                        "고가 장소로 변경합니다.",
                        List.of(new ItineraryItemChangeCommand(
                                ItemChangeOperation.UPDATE, plan.dayId(), plan.itemId(), expensivePlaceId,
                                LocalTime.of(10, 0), LocalTime.of(11, 0), null, "사용자 요청"
                        ))
                ));

        mockMvc.perform(post("/api/itineraries/" + plan.itineraryId() + "/ai-revisions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruction\":\"비싼 장소로 변경해줘\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("G007"));

        mockMvc.perform(get("/api/itineraries/" + plan.itineraryId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[0].items[0].placeId").value(originalPlaceId))
                .andExpect(jsonPath("$.data.days[0].items[0].startTime").value("09:00:00"));
    }

    @Test
    @DisplayName("VIEWER 요청은 일정 내용을 AI 공급자에 보내기 전에 차단한다")
    void revise_rejectsViewerBeforeProviderCall() throws Exception {
        String ownerToken = signup("ai-owner-viewer@example.com");
        String viewerToken = signup("ai-viewer@example.com");
        long placeId = createPlace(ownerToken, "장소", "AI도시C", 10000, "09:00", "18:00");
        PlanIds plan = createPlan(ownerToken, "AI도시C", 50000, placeId);
        mockMvc.perform(post("/api/itineraries/" + plan.itineraryId() + "/participants")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberEmail\":\"ai-viewer@example.com\",\"participantRole\":\"VIEWER\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/itineraries/" + plan.itineraryId() + "/ai-revisions")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruction\":\"일정을 바꿔줘\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("P002"));

        verify(provider, never()).propose(any(AiRevisionContext.class), any(String.class));
    }

    @Test
    @DisplayName("AI 공급자 설정이 없으면 G005를 반환한다")
    void revise_withoutProvider_returnsConfigurationError() throws Exception {
        when(provider.isAvailable()).thenReturn(false);
        String token = signup("ai-owner-unavailable@example.com");

        mockMvc.perform(post("/api/itineraries/999/ai-revisions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruction\":\"일정을 바꿔줘\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("G005"));
    }

    private String signup(String email) throws Exception {
        return AuthTestHelper.signupAndGetToken(mockMvc, objectMapper, email, "password123", "AI계획자");
    }

    private long createPlace(
            String token, String name, String city, int cost, String openingTime, String closingTime
    ) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("category", "SIGHT");
        body.put("region", city);
        body.put("openingTime", openingTime);
        body.put("closingTime", closingTime);
        body.put("estimatedCost", cost);
        MvcResult result = mockMvc.perform(post("/api/places")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("data").get("id").asLong();
    }

    private PlanIds createPlan(String token, String city, int budget, long placeId) throws Exception {
        LocalDate date = LocalDate.now().plusDays(20);
        MvcResult itineraryResult = mockMvc.perform(post("/api/itineraries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", city + " AI 일정", "city", city,
                                "startDate", date.toString(), "endDate", date.toString(), "budget", budget
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        long itineraryId = objectMapper.readTree(
                itineraryResult.getResponse().getContentAsString(StandardCharsets.UTF_8)
        ).get("data").get("id").asLong();

        MvcResult dayResult = mockMvc.perform(post("/api/itineraries/" + itineraryId + "/days")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dayNumber", 1, "date", date.toString()
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        long dayId = objectMapper.readTree(dayResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("data").get("id").asLong();

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("placeId", placeId);
        item.put("visitOrder", 1);
        item.put("startTime", "09:00");
        item.put("endTime", "10:00");
        item.put("estimatedCost", 10000);
        MvcResult itemResult = mockMvc.perform(post(
                        "/api/itineraries/" + itineraryId + "/days/" + dayId + "/items"
                ).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andReturn();
        long itemId = objectMapper.readTree(itemResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("data").get("id").asLong();
        return new PlanIds(itineraryId, dayId, itemId);
    }

    private record PlanIds(long itineraryId, long dayId, long itemId) {
    }
}
