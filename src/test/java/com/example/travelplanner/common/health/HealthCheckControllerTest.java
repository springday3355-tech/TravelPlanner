package com.example.travelplanner.common.health;

import com.example.travelplanner.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 하네스 검증용 예시 테스트.
 * ControllerTestSupport를 상속하는 것만으로 MockMvc/ObjectMapper가 주입되고,
 * 컨트롤러 -> ApiResponse -> JSON 직렬화까지 전체 경로가 정상 동작하는지 확인한다.
 */
class HealthCheckControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("GET /api/health 호출 시 성공 응답을 반환한다")
    void health() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data").value("OK"));
    }
}
