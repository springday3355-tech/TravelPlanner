package com.example.travelplanner.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 액세스 토큰 발급/검증 담당.
 * 인증 방식은 STATELESS(토큰 기반)로 확정되었다 (SecurityConfig 최초 작성 시점부터 전제된 방향).
 * 참고: docs/architecture/TravelPlanner-SA.md 9절, docs/domain/트래블메이커-정책정리본.md
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_MEMBER_ID = "memberId";

    private final SecretKey key;
    private final long accessTokenValiditySeconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-seconds}") long accessTokenValiditySeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public String createAccessToken(Long memberId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValiditySeconds * 1000);

        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_MEMBER_ID, memberId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 토큰을 검증하고 memberId를 반환한다. 유효하지 않으면 JwtException을 던진다(호출부에서 처리).
     */
    public Long getMemberId(String token) {
        Claims claims = parseClaims(token);
        return claims.get(CLAIM_MEMBER_ID, Long.class);
    }

    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
