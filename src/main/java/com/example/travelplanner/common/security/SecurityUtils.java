package com.example.travelplanner.common.security;

import com.example.travelplanner.common.exception.BusinessException;
import com.example.travelplanner.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * JwtAuthenticationFilter가 SecurityContext에 채운 memberId(principal)를 꺼내는 헬퍼.
 * 컨트롤러/서비스에서 SecurityContextHolder를 직접 다루지 않고 이 클래스를 통해서만 접근한다.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long memberId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return memberId;
    }
}
