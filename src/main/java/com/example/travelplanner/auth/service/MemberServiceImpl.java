package com.example.travelplanner.auth.service;

import com.example.travelplanner.auth.dto.LoginRequest;
import com.example.travelplanner.auth.dto.LoginResponse;
import com.example.travelplanner.auth.dto.MemberResponse;
import com.example.travelplanner.auth.dto.MemberSummary;
import com.example.travelplanner.auth.dto.SignupRequest;
import com.example.travelplanner.auth.entity.Member;
import com.example.travelplanner.auth.repository.MemberRepository;
import com.example.travelplanner.common.exception.BusinessException;
import com.example.travelplanner.common.exception.ErrorCode;
import com.example.travelplanner.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public MemberResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATE);
        }

        Member member = Member.of(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname()
        );
        Member saved = memberRepository.save(member);
        return MemberResponse.from(saved);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getEmail());
        return new LoginResponse(accessToken, MemberResponse.from(member));
    }

    @Override
    public MemberResponse getMe(Long memberId) {
        return MemberResponse.from(getMemberOrThrow(memberId));
    }

    @Override
    public MemberSummary getSummaryByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return new MemberSummary(member.getId(), member.getNickname());
    }

    @Override
    public MemberSummary getSummary(Long memberId) {
        Member member = getMemberOrThrow(memberId);
        return new MemberSummary(member.getId(), member.getNickname());
    }

    private Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
