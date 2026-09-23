package com.example.travelplanner.auth.service;

import com.example.travelplanner.auth.dto.LoginRequest;
import com.example.travelplanner.auth.dto.LoginResponse;
import com.example.travelplanner.auth.dto.MemberResponse;
import com.example.travelplanner.auth.dto.MemberSummary;
import com.example.travelplanner.auth.dto.SignupRequest;

/**
 * 다른 도메인은 이 인터페이스를 통해서만 회원 정보에 접근한다(SA 문서 3절, 도메인 간 직접 참조 금지).
 */
public interface MemberService {

    MemberResponse signup(SignupRequest request);

    LoginResponse login(LoginRequest request);

    MemberResponse getMe(Long memberId);

    /**
     * 이메일로 회원을 찾아 요약 정보를 반환한다. 없으면 BusinessException(MEMBER_NOT_FOUND).
     * itinerary 도메인의 동행자 초대(이메일 기반)에서 사용한다.
     */
    MemberSummary getSummaryByEmail(String email);

    MemberSummary getSummary(Long memberId);
}
