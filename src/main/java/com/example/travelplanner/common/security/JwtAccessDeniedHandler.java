package com.example.travelplanner.common.security;

import com.example.travelplanner.common.exception.ErrorCode;
import com.example.travelplanner.common.response.ApiResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증은 됐지만 권한이 없는 경우(Spring Security 레벨)의 응답도 ApiResponse 포맷으로 통일한다.
 * 일정 소유자/동행자 권한처럼 도메인 데이터에 의존하는 인가는 서비스 계층의 BusinessException으로 처리하고,
 * 이 핸들러는 Security 설정(authorizeHttpRequests) 레벨의 거부만 담당한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(ErrorCode.FORBIDDEN.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(ErrorCode.FORBIDDEN)));
    }
}
