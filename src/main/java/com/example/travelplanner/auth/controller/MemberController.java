package com.example.travelplanner.auth.controller;

import com.example.travelplanner.auth.dto.MemberResponse;
import com.example.travelplanner.auth.service.MemberService;
import com.example.travelplanner.common.response.ApiResponse;
import com.example.travelplanner.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ApiResponse<MemberResponse> me() {
        Long memberId = SecurityUtils.getCurrentMemberId();
        return ApiResponse.success(memberService.getMe(memberId));
    }
}
