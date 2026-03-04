package com.jobboard.auth_service.service;

import com.jobboard.auth_service.dto.AuthResponse;
import com.jobboard.auth_service.dto.LoginRequest;
import com.jobboard.auth_service.dto.RegisterRequest;
import com.jobboard.auth_service.entity.User;
import com.jobboard.auth_service.exception.EmailAlreadyExistsException;
import com.jobboard.auth_service.exception.InvalidCredentialsException;
import com.jobboard.auth_service.exception.InvalidTokenException;
import com.jobboard.auth_service.repository.UserRepository;
import com.jobboard.auth_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();
        user = userRepository.save(user);

        return generateTokens(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return generateTokens(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new InvalidTokenException();
        }

        Long userId = jwtUtil.extractUserId(refreshToken);
        String storedToken = redisTemplate.opsForValue().get("refresh_token:" + userId);

        if (!refreshToken.equals(storedToken)) {
            // Token reuse detected — possible theft, invalidate all sessions
            redisTemplate.delete("refresh_token:" + userId);
            throw new InvalidTokenException();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(InvalidTokenException::new);

        return generateTokens(user);
    }

    public void logout(String accessToken) {
        if (!jwtUtil.validateToken(accessToken)) {
            throw new InvalidTokenException();
        }

        Long userId = jwtUtil.extractUserId(accessToken);

        // Blacklist access token until expired
        long ttl = jwtUtil.getExpiration(accessToken) - System.currentTimeMillis();
        if (ttl > 0) {
            redisTemplate.opsForValue().set("blacklist:" + accessToken, "1", ttl, TimeUnit.MILLISECONDS);
        }

        // Delete refresh token
        redisTemplate.delete("refresh_token:" + userId);
    }

    public String getUserEmail(Long userId) {
        return userRepository.findById(userId)
                .map(User::getEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    private AuthResponse generateTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Save refresh token vào Redis, TTL 7 day
        redisTemplate.opsForValue().set("refresh_token:" + user.getId(), refreshToken, 7, TimeUnit.DAYS);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
