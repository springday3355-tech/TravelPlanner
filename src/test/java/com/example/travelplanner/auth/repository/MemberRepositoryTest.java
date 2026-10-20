package com.example.travelplanner.auth.repository;

import com.example.travelplanner.auth.entity.Member;
import com.example.travelplanner.support.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("이메일로 회원을 조회할 수 있고, BaseTimeEntity의 createdAt이 자동으로 채워진다")
    void findByEmail_returnsSavedMember() {
        Member member = Member.of("repo-test@example.com", "encoded-password", "테스트닉네임");
        memberRepository.save(member);

        Optional<Member> found = memberRepository.findByEmail("repo-test@example.com");

        assertTrue(found.isPresent());
        assertEquals("테스트닉네임", found.get().getNickname());
        assertNotNull(found.get().getCreatedAt());
    }

    @Test
    @DisplayName("존재하지 않는 이메일 중복 검사는 false를 반환한다")
    void existsByEmail_returnsFalse_whenNotExists() {
        assertFalse(memberRepository.existsByEmail("no-such-email@example.com"));
    }
}
