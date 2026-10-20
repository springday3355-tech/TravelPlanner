package com.example.travelplanner.auth.dto;

/**
 * 다른 도메인(itinerary 등)이 MemberService를 통해서만 회원 정보를 얻을 때 쓰는 최소 정보.
 * 비밀번호 등 민감 정보를 포함하지 않는다.
 */
public record MemberSummary(
        Long id,
        String nickname
) {
}
