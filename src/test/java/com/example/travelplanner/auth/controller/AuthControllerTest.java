package com.example.travelplanner.auth.controller;

import com.example.travelplanner.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("회원가입에 성공하면 201과 회원 정보를 반환한다")
    void signup_success() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("email", "traveler@example.com", "password", "password123", "nickname", "여행자")
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("traveler@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("여행자"));
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 실패 응답(M001)을 반환한다")
    void signup_duplicateEmail_fails() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("email", "dup@example.com", "password", "password123", "nickname", "중복")
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("M001"));
    }

    @Test
    @DisplayName("로그인에 성공하면 accessToken을 반환한다")
    void login_success() throws Exception {
        String signupBody = objectMapper.writeValueAsString(
                Map.of("email", "login@example.com", "password", "password123", "nickname", "로그인")
        );
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                Map.of("email", "login@example.com", "password", "password123")
        );
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.member.email").value("login@example.com"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 실패 응답(M003)을 반환한다")
    void login_wrongPassword_fails() throws Exception {
        String signupBody = objectMapper.writeValueAsString(
                Map.of("email", "wrongpw@example.com", "password", "password123", "nickname", "테스트")
        );
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                Map.of("email", "wrongpw@example.com", "password", "wrong-password")
        );
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("M003"));
    }

    @Test
    @DisplayName("토큰 없이 /api/members/me 를 호출하면 401과 ApiResponse 포맷 에러를 반환한다")
    void me_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("A001"));
    }
}
