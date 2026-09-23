package com.example.travelplanner.support;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컨트롤러 테스트에서 회원가입+로그인을 거쳐 JWT 액세스 토큰을 얻기 위한 공용 헬퍼.
 * ControllerTestSupport를 상속하는 테스트에서 이 헬퍼를 사용해 인증이 필요한 API를 검증한다.
 */
public final class AuthTestHelper {

    private AuthTestHelper() {
    }

    public static String signupAndGetToken(
            MockMvc mockMvc, ObjectMapper objectMapper, String email, String password, String nickname
    ) throws Exception {
        String signupBody = objectMapper.writeValueAsString(
                Map.of("email", email, "password", password, "nickname", nickname)
        );
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                Map.of("email", email, "password", password)
        );
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        return json.get("data").get("accessToken").asText();
    }
}
