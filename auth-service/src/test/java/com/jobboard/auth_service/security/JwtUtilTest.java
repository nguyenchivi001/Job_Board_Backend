package com.jobboard.auth_service.security;

import com.jobboard.auth_service.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION = 86400000L;
    private static final long REFRESH_EXPIRATION = 604800000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION, REFRESH_EXPIRATION);
    }

    @Test
    void generateAccessToken_thenValidateToken_returnsTrue() {
        String token = jwtUtil.generateAccessToken(1L, "user@test.com", Role.CANDIDATE);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void generateRefreshToken_thenValidateToken_returnsTrue() {
        String token = jwtUtil.generateRefreshToken(1L);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void generateAccessToken_extractUserId_matchesInput() {
        String token = jwtUtil.generateAccessToken(42L, "user@test.com", Role.EMPLOYER);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void generateAccessToken_extractEmail_matchesInput() {
        String token = jwtUtil.generateAccessToken(1L, "test@example.com", Role.CANDIDATE);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("test@example.com");
    }

    @Test
    void generateAccessToken_extractRole_matchesInput() {
        String token = jwtUtil.generateAccessToken(1L, "user@test.com", Role.EMPLOYER);
        assertThat(jwtUtil.extractRole(token)).isEqualTo(Role.EMPLOYER);
    }

    @Test
    void expiredToken_validateToken_returnsFalse() {
        // Negative expiration → token is already expired at creation time
        JwtUtil expiredJwtUtil = new JwtUtil(SECRET, -1000L, REFRESH_EXPIRATION);
        String token = expiredJwtUtil.generateAccessToken(1L, "user@test.com", Role.CANDIDATE);
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void invalidToken_validateToken_returnsFalse() {
        assertThat(jwtUtil.validateToken("not.a.valid.token")).isFalse();
    }

    @Test
    void generateAccessToken_getExpiration_returnsFutureTime() {
        String token = jwtUtil.generateAccessToken(1L, "user@test.com", Role.CANDIDATE);
        assertThat(jwtUtil.getExpiration(token)).isGreaterThan(System.currentTimeMillis());
    }
}
