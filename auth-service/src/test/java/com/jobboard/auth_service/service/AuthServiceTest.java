package com.jobboard.auth_service.service;

import com.jobboard.auth_service.dto.AuthResponse;
import com.jobboard.auth_service.dto.LoginRequest;
import com.jobboard.auth_service.dto.RegisterRequest;
import com.jobboard.auth_service.entity.User;
import com.jobboard.auth_service.enums.Role;
import com.jobboard.auth_service.exception.EmailAlreadyExistsException;
import com.jobboard.auth_service.exception.InvalidCredentialsException;
import com.jobboard.auth_service.exception.InvalidTokenException;
import com.jobboard.auth_service.repository.UserRepository;
import com.jobboard.auth_service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate redisTemplate;

    @SuppressWarnings("unchecked")
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@test.com")
                .password("encoded_password")
                .role(Role.CANDIDATE)
                .build();
    }

    @Test
    void register_newEmail_returnsAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");
        request.setRole(Role.CANDIDATE);

        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(1L, "user@test.com", Role.CANDIDATE)).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(1L)).thenReturn("refresh_token");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
        assertThat(response.getEmail()).isEqualTo("user@test.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_existingEmail_throwsEmailAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@test.com");
        request.setPassword("password123");
        request.setRole(Role.CANDIDATE);

        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_returnsAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(1L, "user@test.com", Role.CANDIDATE)).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(1L)).thenReturn("refresh_token");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getEmail()).isEqualTo("user@test.com");
    }

    @Test
    void login_emailNotFound_throwsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("wrong_password");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refreshToken_validToken_matchesRedis_returnsNewTokens() {
        String refreshToken = "valid_refresh_token";

        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUserId(refreshToken)).thenReturn(1L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("refresh_token:1")).thenReturn(refreshToken);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateAccessToken(1L, "user@test.com", Role.CANDIDATE)).thenReturn("new_access_token");
        when(jwtUtil.generateRefreshToken(1L)).thenReturn("new_refresh_token");

        AuthResponse response = authService.refreshToken(refreshToken);

        assertThat(response.getAccessToken()).isEqualTo("new_access_token");
        assertThat(response.getRefreshToken()).isEqualTo("new_refresh_token");
    }

    @Test
    void refreshToken_invalidToken_throwsInvalidTokenException() {
        when(jwtUtil.validateToken("bad_token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("bad_token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refreshToken_tokenReuse_deletesRedisKey_throws() {
        String requestToken = "used_token";
        String storedToken = "different_stored_token";

        when(jwtUtil.validateToken(requestToken)).thenReturn(true);
        when(jwtUtil.extractUserId(requestToken)).thenReturn(1L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("refresh_token:1")).thenReturn(storedToken);

        assertThatThrownBy(() -> authService.refreshToken(requestToken))
                .isInstanceOf(InvalidTokenException.class);
        verify(redisTemplate).delete("refresh_token:1");
    }

    @Test
    void logout_validToken_blacklistsAndDeletesRefresh() {
        String accessToken = "valid_access_token";
        long futureExpiry = System.currentTimeMillis() + 3600000L;

        when(jwtUtil.validateToken(accessToken)).thenReturn(true);
        when(jwtUtil.extractUserId(accessToken)).thenReturn(1L);
        when(jwtUtil.getExpiration(accessToken)).thenReturn(futureExpiry);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        authService.logout(accessToken);

        verify(valueOps).set(eq("blacklist:" + accessToken), eq("1"), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(redisTemplate).delete("refresh_token:1");
    }

    @Test
    void logout_invalidToken_throwsInvalidTokenException() {
        when(jwtUtil.validateToken("bad_token")).thenReturn(false);

        assertThatThrownBy(() -> authService.logout("bad_token"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
