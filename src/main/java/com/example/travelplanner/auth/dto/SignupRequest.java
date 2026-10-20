package com.example.travelplanner.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 최소 길이(8자)는 정책 문서에 상세 규칙이 확정되기 전까지의
 * 잠정 기본값이다 (docs/domain/트래블메이커-정책정리본.md 1절 "결정 대기" 참고).
 */
public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 50) String nickname
) {
}
