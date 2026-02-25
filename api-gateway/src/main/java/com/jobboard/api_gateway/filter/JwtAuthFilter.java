package com.jobboard.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    // Các path không cần JWT — auth-service tự xử lý xác thực nội bộ
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            sendUnauthorized(response, "Invalid or expired token");
            return;
        }

        // Kiểm tra token đã bị blacklist chưa (user đã logout)
        Boolean blacklisted = redisTemplate.hasKey("blacklist:" + token);
        if (Boolean.TRUE.equals(blacklisted)) {
            sendUnauthorized(response, "Token has been revoked");
            return;
        }

        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        // Inject X-User-Id và X-User-Role vào request trước khi forward
        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("X-User-Id", userId);
        extraHeaders.put("X-User-Role", role);

        filterChain.doFilter(new HeaderMutatingRequestWrapper(request, extraHeaders), response);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"status":401,"error":"Unauthorized","message":"%s"}
                """.formatted(message));
    }

    // Wrapper để thêm custom headers vào request (HttpServletRequest mặc định immutable)
    private static class HeaderMutatingRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, String> additionalHeaders;

        HeaderMutatingRequestWrapper(HttpServletRequest request, Map<String, String> additionalHeaders) {
            super(request);
            this.additionalHeaders = new HashMap<>(additionalHeaders);
        }

        @Override
        public String getHeader(String name) {
            if (additionalHeaders.containsKey(name)) {
                return additionalHeaders.get(name);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            names.addAll(additionalHeaders.keySet());
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (additionalHeaders.containsKey(name)) {
                return Collections.enumeration(List.of(additionalHeaders.get(name)));
            }
            return super.getHeaders(name);
        }
    }
}
