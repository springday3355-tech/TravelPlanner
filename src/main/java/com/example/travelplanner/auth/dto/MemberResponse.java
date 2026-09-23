package com.example.travelplanner.auth.dto;

import com.example.travelplanner.auth.entity.Member;

public record MemberResponse(
        Long id,
        String email,
        String nickname,
        String role
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole().name()
        );
    }
}
