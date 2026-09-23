package com.example.travelplanner.auth.dto;

/**
 * docs/architecture/TravelPlanner-API명세.md 1절: 인증 방식이 JWT로 확정됨에 따라
 * accessToken을 반환하는 것으로 스펙을 확정한다(문서도 함께 갱신했음).
 */
public record LoginResponse(
        String accessToken,
        MemberResponse member
) {
}
